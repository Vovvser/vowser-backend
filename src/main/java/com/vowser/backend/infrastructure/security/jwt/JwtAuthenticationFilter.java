package com.vowser.backend.infrastructure.security.jwt;

import com.vowser.backend.domain.member.entity.Member;
import com.vowser.backend.domain.member.repository.MemberRepository;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 모든 요청에 대해 JWT 토큰을 검증하고 인증 정보를 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Request Header에서 JWT 토큰 추출
            String token = extractTokenFromRequest(request);
            
            // 2. 토큰이 있고 유효한 경우
            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                
                // 3. Access Token인지 확인
                if (!jwtProvider.isAccessToken(token)) {
                    log.warn("Access Token이 아닙니다: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 4. 토큰에서 회원 ID 추출
                Long memberId = jwtProvider.getMemberIdFromToken(token);
                
                // 5. 회원 정보 조회
                Member member = memberRepository.findById(memberId)
                        .orElse(null);
                        
                if (member != null) {
                    // 6. CustomUserDetails 생성
                    CustomUserDetails userDetails = new CustomUserDetails(member);
                    
                    // 7. Authentication 객체 생성
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities()
                            );
                    
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // 8. SecurityContext에 Authentication 설정
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT 인증 성공: memberId={}, requestURI={}", 
                            memberId, request.getRequestURI());
                } else {
                    log.warn("회원을 찾을 수 없습니다: memberId={}", memberId);
                }
            }
            
        } catch (Exception e) {
            log.error("JWT 인증 필터 처리 중 오류 발생: {}", e.getMessage());
        }
        
        // 9. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
    
    /**
     * Request Header에서 JWT 토큰 추출
     * 
     * @param request HttpServletRequest
     * @return JWT 토큰 (Bearer 제거)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * 필터를 적용하지 않을 경로인지 확인
     * 
     * @param request current HTTP request
     * @return true if should not filter, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // OAuth2 로그인 경로는 필터 제외
        return path.startsWith("/oauth2/") || 
               path.startsWith("/login/oauth2/") ||
               path.equals("/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/health") ||
               path.equals("/error") ||
               path.equals("/favicon.ico");
    }
}