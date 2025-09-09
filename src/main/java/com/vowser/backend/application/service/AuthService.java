package com.vowser.backend.application.service;

import com.vowser.backend.api.dto.member.MemberResponse;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import com.vowser.backend.infrastructure.security.jwt.JwtProvider;
import com.vowser.backend.infrastructure.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final MemberService memberService;
    
    /**
     * 로그아웃 처리
     * Access Token에서 회원 ID 추출 후 Refresh Token 삭제
     */
    public void logout(String accessToken) {
        try {
            if (jwtProvider.validateToken(accessToken)) {
                Long memberId = jwtProvider.getMemberIdFromToken(accessToken);
                refreshTokenService.deleteRefreshToken(memberId);
                log.info("로그아웃 성공: memberId={}", memberId);
            }
        } catch (Exception e) {
            log.warn("로그아웃 처리 중 오류: {}", e.getMessage());
        }
    }
    
    /**
     * memberId로 로그아웃 처리 (기존 호환성 유지)
     */
    public void logout(Long memberId) {
        if (memberId != null) {
            refreshTokenService.deleteRefreshToken(memberId);
            log.info("로그아웃 성공: memberId={}", memberId);
        }
    }
    
    /**
     * 현재 로그인한 사용자 정보 조회
     */
    public MemberResponse getCurrentUserInfo(CustomUserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return memberService.getMemberInfo(userDetails.getMemberId());
    }
}