package com.vowser.backend.infrastructure.security.oauth2;

import com.vowser.backend.domain.member.entity.Member;
import com.vowser.backend.domain.member.repository.MemberRepository;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 * 
 * 핵심 기능:
 * 1. OAuth2 제공자로부터 사용자 정보 조회
 * 2. 이메일로 기존 회원 확인
 * 3. 신규 회원은 자동 가입 처리
 * 4. 기존 회원은 정보 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final MemberRepository memberRepository;
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        // 1. OAuth2 제공자로부터 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("OAuth2 로그인 시도: {}", oAuth2User.getAttributes());
        
        // 2. OAuth2 제공자 확인 (현재는 네이버만 지원)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;
        
        if ("naver".equals(registrationId)) {
            oAuth2Response = new NaverOAuth2Response(oAuth2User.getAttributes());
        } else {
            log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
            throw new OAuth2AuthenticationException("지원하지 않는 로그인 방식입니다.");
        }
        
        // 3. 이메일 및 필수 정보 검증
        String email = oAuth2Response.getEmail();
        String providerId = oAuth2Response.getProviderId();
        String name = oAuth2Response.getName();
        
        if (email == null || email.isEmpty()) {
            log.error("OAuth2 로그인 실패: 이메일 정보가 없습니다.");
            throw new OAuth2AuthenticationException("이메일 정보를 제공하지 않는 계정입니다.");
        }
        
        // 4. 기존 회원 조회 또는 신규 회원 생성
        Member member = memberRepository.findByEmail(email)
                .map(existingMember -> updateMember(existingMember, name))
                .orElseGet(() -> createMember(email, providerId, name));
        
        log.info("OAuth2 로그인 성공: memberId={}, email={}", member.getId(), member.getEmail());
        
        // 5. CustomUserDetails 객체 반환
        return new CustomUserDetails(member, oAuth2User.getAttributes());
    }
    
    /**
     * 신규 회원 생성 (자동 가입)
     */
    private Member createMember(String email, String providerId, String name) {
        Member newMember = Member.builder()
                .email(email)
                .naverId(providerId)
                .name(name != null ? name : "네이버 사용자")
                .build();
        
        Member savedMember = memberRepository.save(newMember);
        log.info("신규 회원 자동 가입 완료: memberId={}, email={}", savedMember.getId(), email);
        
        return savedMember;
    }
    
    /**
     * 기존 회원 정보 업데이트
     */
    private Member updateMember(Member member, String name) {
        // 이름이 변경된 경우에만 업데이트
        if (name != null && !name.equals(member.getName())) {
            member.updateName(name);
            memberRepository.save(member);
            log.info("회원 정보 업데이트: memberId={}, newName={}", member.getId(), name);
        }
        
        return member;
    }
}