package com.vowser.backend.infrastructure.security.dto;

import com.vowser.backend.domain.member.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Spring Security에서 사용하는 사용자 정보
 * UserDetails와 OAuth2User를 모두 구현하여 일반 로그인과 OAuth2 로그인 모두 지원
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {
    
    private final Member member;
    private Map<String, Object> attributes;
    
    /**
     * OAuth2 로그인 시 사용하는 생성자
     */
    public CustomUserDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 모든 사용자는 USER 권한만 가짐 (단순화)
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getPassword() {
        // OAuth2 로그인만 사용하므로 패스워드 없음
        return null;
    }
    
    @Override
    public String getUsername() {
        return member.getEmail();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public String getName() {
        return member.getNaverId();
    }
    
    /**
     * 회원 ID 조회
     */
    public Long getMemberId() {
        return member.getId();
    }
    
    /**
     * 회원 이메일 조회
     */
    public String getEmail() {
        return member.getEmail();
    }
    
    /**
     * 회원 이름 조회
     */
    public String getMemberName() {
        return member.getName();
    }
}