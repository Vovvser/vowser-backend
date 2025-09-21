package com.vowser.backend.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vowser.backend.common.enums.DisabilityType;
import com.vowser.backend.domain.member.entity.AccessibilityProfile;
import com.vowser.backend.domain.member.entity.Member;
import com.vowser.backend.domain.member.repository.AccessibilityProfileRepository;
import com.vowser.backend.domain.member.repository.MemberRepository;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessibilityProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccessibilityProfileRepository accessibilityProfileRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트를 위한 사용자 미리 저장
        testMember = Member.createNaverMember("test@example.com", "testuser", "test-naver-id");
        memberRepository.save(testMember);

        // Spring Security 컨텍스트에 Mock 인증 정보 설정
        CustomUserDetails userDetails = new CustomUserDetails(testMember);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("접근성 프로필 생성 통합 테스트")
    void createMyProfile_Success() throws Exception {
        // given
        String settingsJson = "{\"screen_reader_mode\": \"FULL\"}";
        String requestBody = String.format(
                "{\"disabilityType\": \"%s\", \"settingsJson\": \"%s\"}",
                DisabilityType.VISUAL.name(),
                settingsJson.replace("\"", "\\\""))
        ;

        // when & then
        mockMvc.perform(post("/api/v1/member/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.disabilityType").value(DisabilityType.VISUAL.name()))
                .andExpect(jsonPath("$.data.settingsJson").value(settingsJson))
                .andDo(print());

        // DB 검증
        AccessibilityProfile savedProfile = accessibilityProfileRepository.findByMemberId(testMember.getId()).orElseThrow();
        assertNotNull(savedProfile.getId());
        assertEquals(DisabilityType.VISUAL, savedProfile.getDisabilityType());
        assertNotNull(savedProfile.getConsentAgreedAt());

        // 암호화 검증: DB에 저장된 값이 원본 JSON과 달라야 함
        AccessibilityProfile savedInDb = accessibilityProfileRepository.findById(savedProfile.getId()).orElseThrow();
        assertNotEquals(settingsJson, savedInDb.getProfileSettings());
        System.out.println("Original Settings: " + settingsJson);
        System.out.println("Encrypted Settings in DB: " + savedInDb.getProfileSettings());
    }
}