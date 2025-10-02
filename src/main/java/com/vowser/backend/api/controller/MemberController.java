package com.vowser.backend.api.controller;

import com.vowser.backend.api.dto.member.MemberResponse;
import com.vowser.backend.api.dto.common.ApiResponse;
import com.vowser.backend.application.service.MemberService;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    
    /**
     * 현재 로그인한 회원 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            log.warn("인증되지 않은 사용자의 회원 정보 조회 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<MemberResponse>errorWithType(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."));
        }
        
        Long memberId = userDetails.getMemberId();
        MemberResponse memberResponse = memberService.getMemberInfo(memberId);

        return ResponseEntity.ok(ApiResponse.success(memberResponse));
    }
}