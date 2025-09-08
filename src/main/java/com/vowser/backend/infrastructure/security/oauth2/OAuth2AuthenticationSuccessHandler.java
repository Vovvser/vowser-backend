package com.vowser.backend.infrastructure.security.oauth2;

import com.vowser.backend.infrastructure.security.CustomUserDetails;
import com.vowser.backend.infrastructure.security.jwt.CookieUtil;
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

import java.io.IOException;

/**
 * OAuth2 로그인 성공 핸들러
 * JWT 토큰을 쿠키로 발급 및 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;
    
    @Value("${oauth2.success-redirect-url:http://localhost:3000/auth/success}")
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
        
        // 4. 토큰을 쿠키에 저장
        cookieUtil.addAccessTokenCookie(response, accessToken);
        cookieUtil.addRefreshTokenCookie(response, refreshToken);
        
        log.info("OAuth2 로그인 성공, 토큰을 쿠키로 발급 완료");
        
        // 5. 프론트엔드로 리다이렉트 (토큰은 쿠키에 포함)
        log.info("OAuth2 로그인 성공 후 리다이렉트: {}", successRedirectUrl);
        
        getRedirectStrategy().sendRedirect(request, response, successRedirectUrl);
    }
}