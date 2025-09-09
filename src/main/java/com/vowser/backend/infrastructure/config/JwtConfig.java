package com.vowser.backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 관련 설정 클래스
 * application.yml의 jwt 설정을 매핑
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT 서명에 사용할 비밀키 (Base64 인코딩)
     */
    private String secret;
    
    /**
     * Access Token 유효 시간 (초 단위)
     * 기본값: 30분 (1800초)
     */
    private long accessTokenValidityInSeconds = 1800;
    
    /**
     * Refresh Token 유효 시간 (초 단위)
     * 기본값: 14일 (1209600초)
     */
    private long refreshTokenValidityInSeconds = 1209600;
    
    /**
     * JWT 토큰 발급자
     */
    private String issuer = "vowser-backend";
    
    /**
     * 토큰 헤더 이름
     */
    private String header = "Authorization";
    
    /**
     * 토큰 접두사
     */
    private String prefix = "Bearer ";
    
    /**
     * 쿠키 보안 설정 (HTTPS 전용)
     * 개발 환경에서는 false, 프로덕션에서는 true
     */
    private boolean cookieSecure = false;
    
    /**
     * 쿠키 도메인
     */
    private String cookieDomain = null;
    
    /**
     * 쿠키 경로
     */
    private String cookiePath = "/";
}