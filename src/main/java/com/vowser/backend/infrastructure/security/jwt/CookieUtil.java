package com.vowser.backend.infrastructure.security.jwt;

import com.vowser.backend.infrastructure.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 쿠키 관리 유틸리티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtConfig jwtConfig;

    /**
     * Access Token 쿠키 추가
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("AccessToken", token)
                .httpOnly(true)  // XSS 방지
                .secure(jwtConfig.isCookieSecure())  // HTTPS에서만 전송
                .path("/")
                .maxAge(jwtConfig.getAccessTokenValidityInSeconds())
                .sameSite("Lax")  // CSRF 방지
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Access Token 쿠키 추가 완료");
    }

    /**
     * Refresh Token 쿠키 추가
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", token)
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/")
                .maxAge(jwtConfig.getRefreshTokenValidityInSeconds())
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Refresh Token 쿠키 추가 완료");
    }

    /**
     * 쿠키에서 값 추출
     */
    public String extractCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 쿠키 삭제
     */
    public void deleteCookie(HttpServletResponse response, String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/")
                .maxAge(0)  // 즉시 만료
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("쿠키 삭제: {}", cookieName);
    }

    /**
     * 모든 인증 쿠키 삭제
     */
    public void deleteAllAuthCookies(HttpServletResponse response) {
        deleteCookie(response, "AccessToken");
        deleteCookie(response, "RefreshToken");
        log.debug("모든 인증 쿠키 삭제 완료");
    }
}