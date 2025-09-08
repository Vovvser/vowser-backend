package com.vowser.backend.infrastructure.security.oauth2;

/**
 * OAuth2 제공자별 응답 정보를 추상화하는 인터페이스
 * 각 소셜 로그인 제공자(네이버, 구글, 카카오 등)마다 다른 응답 구조를 통일된 인터페이스로 처리
 */
public interface OAuth2Response {
    
    /**
     * OAuth2 제공자 이름 (예: naver, google, kakao)
     */
    String getProvider();
    
    /**
     * OAuth2 제공자가 발급한 고유 ID
     */
    String getProviderId();
    
    /**
     * 사용자 이메일
     */
    String getEmail();
    
    /**
     * 사용자 이름
     */
    String getName();
}