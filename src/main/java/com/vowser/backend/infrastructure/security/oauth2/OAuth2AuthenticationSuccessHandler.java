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
 * JWT 토큰을 임시 코드로 변환하여 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final TemporaryCodeService temporaryCodeService;

    @Value("${oauth2.success-redirect-url:http://localhost:8888/auth/callback}")
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

        // 4. 임시 인증 코드 생성
        String code = temporaryCodeService.createTemporaryCode(memberId, accessToken, refreshToken);

        log.info("OAuth2 로그인 성공, 임시 코드 생성 완료: code={}", code);

        // 5. 인증 코드를 파라미터로 포함하여 프론트엔드로 리다이렉트
        String redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                .queryParam("code", code)
                .build()
                .toUriString();

        log.info("OAuth2 로그인 성공 후 리다이렉트: {}", redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}