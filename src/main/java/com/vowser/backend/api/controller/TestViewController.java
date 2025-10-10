package com.vowser.backend.api.controller;

import com.vowser.backend.infrastructure.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * API 테스트를 위한 뷰 컨트롤러
 * Thymeleaf를 사용한 테스트 페이지 제공
 */
@Slf4j
@Controller
public class TestViewController {
    
    /**
     * API 테스트 페이지
     */
    @GetMapping("/test")
    public String testPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        
        // 로그인 상태 확인
        boolean isAuthenticated = userDetails != null;
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            model.addAttribute("memberId", userDetails.getMemberId());
            model.addAttribute("email", userDetails.getEmail());
            model.addAttribute("name", userDetails.getName());
            model.addAttribute("phoneNumber", userDetails.getPhoneNumber());
            model.addAttribute("birthdate", userDetails.getBirthdate());
        }
        
        log.info("테스트 페이지 접속: 인증상태={}", isAuthenticated);
        
        return "test";
    }
}
