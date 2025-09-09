package com.vowser.backend.infrastructure.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러
 * 로그인 실패 시 에러 페이지로 리다이렉트
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final String DEFAULT_FAILURE_URL = "http://localhost:3000/login/failure";
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        
        log.error("OAuth2 authentication failed: {}", exception.getMessage());
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }
        
        String targetUrl = UriComponentsBuilder.fromUriString(DEFAULT_FAILURE_URL)
                .queryParam("error", true)
                .queryParam("message", "네이버 로그인에 실패했습니다")
                .build().toUriString();
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}