package com.vowser.backend.api.controller;

import com.vowser.backend.api.dto.common.ApiResponse;
import com.vowser.backend.application.service.AuthService;
import com.vowser.backend.application.service.TokenService;
import com.vowser.backend.common.util.NetworkUtil;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import com.vowser.backend.infrastructure.security.jwt.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 * 쿠키 기반 JWT 토큰 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final TokenService tokenService;
    private final CookieUtil cookieUtil;
    private final NetworkUtil networkUtil;
    
    /**
     * 로그아웃
     * 쿠키에서 토큰 제거 및 Redis에서 Refresh Token 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 쿠키에서 Access Token 추출
        String accessToken = cookieUtil.extractCookie(request, "AccessToken");
        
        if (accessToken != null) {
            // Redis에서 Refresh Token 삭제 등 로그아웃 처리
            authService.logout(accessToken);
        }
        
        // 모든 인증 쿠키 삭제
        cookieUtil.deleteAllAuthCookies(response);
        
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
    
    /**
     * 토큰 갱신
     * 쿠키의 Refresh Token으로 새로운 Access Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 쿠키에서 Refresh Token 추출
        String refreshToken = cookieUtil.extractCookie(request, "RefreshToken");
        
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body("리프레시 토큰이 없습니다.");
        }
        
        try {
            // 클라이언트 IP 추출
            String clientIp = networkUtil.getClientIp(request);
            
            // 토큰 갱신
            var tokenDto = tokenService.refreshAccessToken(refreshToken, clientIp);
            
            // 새로운 토큰을 쿠키에 저장
            cookieUtil.addAccessTokenCookie(response, tokenDto.getAccessToken());
            cookieUtil.addRefreshTokenCookie(response, tokenDto.getRefreshToken());
            
            return ResponseEntity.ok("토큰이 갱신되었습니다.");
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body("토큰 갱신에 실패했습니다.");
        }
    }
    
    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.UNAUTHORIZED, "인증이 필요합니다."));
        }
        
        var userInfo = authService.getCurrentUserInfo(userDetails);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
}