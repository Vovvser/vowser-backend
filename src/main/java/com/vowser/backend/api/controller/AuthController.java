package com.vowser.backend.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vowser.backend.api.dto.auth.TokenResponse;
import com.vowser.backend.api.dto.common.ApiResponse;
import com.vowser.backend.application.service.AuthService;
import com.vowser.backend.application.service.TokenService;
import com.vowser.backend.common.util.NetworkUtil;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import com.vowser.backend.infrastructure.security.jwt.CookieUtil;
import com.vowser.backend.infrastructure.security.oauth2.TemporaryCodeService;
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
    private final TemporaryCodeService temporaryCodeService;
    private final ObjectMapper objectMapper;
    
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
     * Authorization 헤더의 Refresh Token으로 새로운 Access Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String refreshToken = null;

        // Authorization 헤더에서 Refresh Token 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        }

        // 헤더에 없으면 쿠키에서 추출
        if (refreshToken == null) {
            refreshToken = cookieUtil.extractCookie(request, "RefreshToken");
        }

        if (refreshToken == null) {
			return ResponseEntity.status(401)
					.body("리프레시 토큰이 없습니다.");
        }

        try {
            // 클라이언트 IP 추출
            String clientIp = networkUtil.getClientIp(request);

            // 토큰 갱신
            var tokenDto = tokenService.refreshAccessToken(refreshToken, clientIp);

            // JSON으로 반환
            TokenResponse response = TokenResponse.builder()
                    .accessToken(tokenDto.getAccessToken())
                    .refreshToken(tokenDto.getRefreshToken())
                    .tokenType("Bearer")
                    .build();
			
			return ResponseEntity.ok(ApiResponse.success(response));
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

    /**
     * OAuth2 인증 코드를 토큰으로 교환
     */
    @PostMapping("/token/exchange")
    public ResponseEntity<?> exchangeCodeForToken(@RequestParam("code") String code) {
        log.info("토큰 교환 요청: code={}", code);

        try {
            String tokenData = temporaryCodeService.consumeCode(code);

            if (tokenData == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(org.springframework.http.HttpStatus.UNAUTHORIZED,
                                "유효하지 않거나 만료된 인증 코드입니다."));
            }

            // JSON 파싱하여 토큰 추출
            var tokenMap = objectMapper.readValue(tokenData, java.util.Map.class);
            String accessToken = (String) tokenMap.get("accessToken");
            String refreshToken = (String) tokenMap.get("refreshToken");

            log.info("토큰 교환 성공: memberId={}", tokenMap.get("memberId"));

            return ResponseEntity.ok(ApiResponse.success(TokenResponse.builder()
					.accessToken(accessToken)
					.refreshToken(refreshToken)
					.tokenType("Bearer")
					.build()));

        } catch (Exception e) {
            log.error("토큰 교환 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                            "토큰 교환 중 오류가 발생했습니다."));
        }
    }
}