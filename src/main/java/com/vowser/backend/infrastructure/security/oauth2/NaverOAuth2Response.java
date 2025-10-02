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
 *     "email": "openapi@naver.com",
 *     "mobile": "010-1234-5678"
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

    @Override
    public String getPhoneNumber() {
        Map<String, Object> response = (Map<String, Object>) attribute.get("response");
        if (response == null) {
            return null;
        }
        // 네이버는 'mobile' 필드로 휴대폰 번호를 제공
        return (String) response.get("mobile");
    }
}