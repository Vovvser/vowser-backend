package com.vowser.backend.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token Redis 저장 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    /**
     * Refresh Token 저장
     * 
     * @param memberId 회원 ID
     * @param refreshToken Refresh Token
     */
    public void saveRefreshToken(Long memberId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        
        // 토큰의 남은 유효시간 계산
        long expiryMillis = jwtProvider.getTokenExpiry(refreshToken);
        
        if (expiryMillis > 0) {
            // Redis에 저장 (유효시간과 함께)
            redisTemplate.opsForValue().set(
                    key, 
                    refreshToken, 
                    expiryMillis, 
                    TimeUnit.MILLISECONDS
            );
            
            log.info("Refresh Token 저장 완료: memberId={}, expiry={}ms", 
                    memberId, expiryMillis);
        } else {
            log.warn("유효하지 않은 Refresh Token: memberId={}", memberId);
        }
    }
    
    /**
     * Refresh Token 조회
     * 
     * @param memberId 회원 ID
     * @return Refresh Token (없으면 null)
     */
    public String getRefreshToken(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        String refreshToken = redisTemplate.opsForValue().get(key);
        
        if (refreshToken != null) {
            log.debug("Refresh Token 조회 성공: memberId={}", memberId);
        } else {
            log.debug("Refresh Token 없음: memberId={}", memberId);
        }
        
        return refreshToken;
    }
    
    /**
     * Refresh Token 검증
     * Redis에 저장된 토큰과 일치하는지 확인
     * 
     * @param memberId 회원 ID
     * @param refreshToken 검증할 Refresh Token
     * @return 유효 여부
     */
    public boolean validateRefreshToken(Long memberId, String refreshToken) {
        String storedToken = getRefreshToken(memberId);
        
        if (storedToken == null) {
            log.warn("저장된 Refresh Token이 없습니다: memberId={}", memberId);
            return false;
        }
        
        if (!storedToken.equals(refreshToken)) {
            log.warn("Refresh Token 불일치: memberId={}", memberId);
            return false;
        }
        
        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token: memberId={}", memberId);
            deleteRefreshToken(memberId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     * 
     * @param memberId 회원 ID
     */
    public void deleteRefreshToken(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Refresh Token 삭제 완료: memberId={}", memberId);
        } else {
            log.debug("삭제할 Refresh Token 없음: memberId={}", memberId);
        }
    }
    
    /**
     * Refresh Token 갱신
     * 기존 토큰 삭제 후 새 토큰 저장
     * 
     * @param memberId 회원 ID
     * @param newRefreshToken 새로운 Refresh Token
     */
    public void updateRefreshToken(Long memberId, String newRefreshToken) {
        deleteRefreshToken(memberId);
        saveRefreshToken(memberId, newRefreshToken);
        log.info("Refresh Token 갱신 완료: memberId={}", memberId);
    }
    
    /**
     * Refresh Token 존재 여부 확인
     * 
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    public boolean hasRefreshToken(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Refresh Token TTL 조회 (초 단위)
     * 
     * @param memberId 회원 ID
     * @return TTL (초), 키가 없으면 -2, 만료시간이 없으면 -1
     */
    public Long getRefreshTokenTTL(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}