package com.vowser.backend.infrastructure.security.oauth2;

import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 네이버 OAuth2 로그인 응답 처리 클래스
 * 
 * 네이버 API 응답 구조:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "32742776",
 *     "name": "홍길동",
 *     "email": "openapi@naver.com"
 *   }
 * }
 */
@RequiredArgsConstructor
public class NaverOAuth2Response implements OAuth2Response {
    
    private final Map<String, Object> attribute;
    
    @Override
    public String getProvider() {
        return "naver";
    }
    
    @Override
    public String getProviderId() {
        Map<String, Object> response = (Map<String, Object>) attribute.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("id");
    }
    
    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attribute.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("email");
    }
    
    @Override
    public String getName() {
        Map<String, Object> response = (Map<String, Object>) attribute.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("name");
    }
}