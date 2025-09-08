package com.vowser.backend.infrastructure.security.oauth2;

import com.vowser.backend.infrastructure.security.CustomUserDetails;
import com.vowser.backend.infrastructure.security.jwt.JwtProvider;
import com.vowser.backend.infrastructure.security.jwt.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 핸들러
 * JWT 토큰 발급 및 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${oauth2.success-redirect-url:http://localhost:3000/auth/callback}")
    private String successRedirectUrl;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }
        
        // 1. 인증된 사용자 정보 추출
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getMemberId();
        String email = userDetails.getEmail();
        
        log.info("OAuth2 로그인 성공: memberId={}, email={}", memberId, email);
        
        // 2. JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(memberId, email);
        String refreshToken = jwtProvider.createRefreshToken(memberId);
        
        // 3. Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(memberId, refreshToken);
        
        // 4. 프론트엔드로 토큰과 함께 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();
        
        log.info("OAuth2 로그인 성공 후 리다이렉트: {}", targetUrl);
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}