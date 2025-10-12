package com.vowser.backend.infrastructure.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth 임시 코드 저장 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryCodeService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CODE_PREFIX = "oauth_code:";
    private static final long CODE_EXPIRY_MINUTES = 5; // 유효 기간 5분

    /**
     * 임시 코드 생성 및 저장
     *
     * @param memberId 회원 ID
     * @param accessToken Access Token
     * @param refreshToken Refresh Token
     * @return 생성된 임시 코드
     */
    public String createTemporaryCode(Long memberId, String accessToken, String refreshToken) {
        String code = UUID.randomUUID().toString();
        String key = CODE_PREFIX + code;

        // 토큰들을 JSON 형태로 저장
        String tokenData = String.format("{\"memberId\":%d,\"accessToken\":\"%s\",\"refreshToken\":\"%s\"}",
                memberId, accessToken, refreshToken);

        redisTemplate.opsForValue().set(
                key,
                tokenData,
                CODE_EXPIRY_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("임시 코드 생성 완료: code={}, memberId={}", code, memberId);
        return code;
    }

    /**
     * 임시 코드로 토큰 정보 조회
     *
     * @param code 임시 코드
     * @return 토큰 데이터 (형식: {"memberId":1,"accessToken":"...","refreshToken":"..."})
     */
    public String consumeCode(String code) {
        String key = CODE_PREFIX + code;
        String tokenData = redisTemplate.opsForValue().get(key);

        if (tokenData != null) {
            redisTemplate.delete(key); // 즉시 삭제
            log.info("임시 코드 사용 완료: code={}", code);
            return tokenData;
        } else {
            log.warn("유효하지 않거나 만료된 임시 코드: code={}", code);
            return null;
        }
    }
}
