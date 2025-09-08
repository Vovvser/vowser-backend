package com.vowser.backend.application.service;

import com.vowser.backend.api.dto.auth.TokenResponse;
import com.vowser.backend.common.exception.AuthException;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.domain.member.entity.Member;
import com.vowser.backend.infrastructure.security.jwt.JwtProvider;
import com.vowser.backend.infrastructure.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {
    
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberService memberService;
    
    /**
     * Refresh Token으로 새로운 Access Token 발급
     * @param refreshToken Refresh Token
     * @param clientIp 클라이언트 IP (보안 검증용)
     */
    public TokenResponse refreshAccessToken(String refreshToken, String clientIp) {
        // 1. Refresh Token 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token");
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
        
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            log.warn("Refresh Token이 아닙니다");
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
        
        // 2. Refresh Token에서 회원 ID 추출
        Long memberId = jwtProvider.getMemberIdFromToken(refreshToken);
        
        // 3. Redis에 저장된 Refresh Token과 비교
        if (!refreshTokenService.validateRefreshToken(memberId, refreshToken)) {
            log.warn("Refresh Token 불일치: memberId={}", memberId);
            throw new AuthException(ErrorCode.TOKEN_MISMATCH);
        }
        
        // 4. 회원 정보 조회
        Member member = memberService.getMember(memberId);
        
        // 5. 새로운 Access Token 발급
        String newAccessToken = jwtProvider.createAccessToken(memberId, member.getEmail());
        
        // 6. Refresh Token 재사용 (선택사항: 새로 발급할 수도 있음)
        // String newRefreshToken = jwtProvider.createRefreshToken(memberId);
        // refreshTokenService.updateRefreshToken(memberId, newRefreshToken);
        
        log.info("토큰 갱신 성공: memberId={}", memberId);
        
        return TokenResponse.of(
                newAccessToken,
                refreshToken,  // 기존 Refresh Token 재사용
                1800L  // 30분
        );
    }
    
    /**
     * Request Header에서 JWT 토큰 추출
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}