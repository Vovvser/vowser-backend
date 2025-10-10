package com.vowser.backend.infrastructure.security;

import com.vowser.backend.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Spring Security의 인증 정보를 담는 클래스
 * UserDetails와 OAuth2User 인터페이스를 모두 구현하여 
 * 일반 로그인과 소셜 로그인을 통합 처리
 */
@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {
    
    private final Member member;
    private final Map<String, Object> attributes;
    
    /**
     * OAuth2 로그인용 생성자
     */
    public CustomUserDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }
    
    /**
     * 일반 로그인용 생성자 (JWT 인증 시 사용)
     */
    public CustomUserDetails(Member member) {
        this.member = member;
        this.attributes = null;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한 구분 없이 일반 사용자만 존재
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getPassword() {
        // OAuth2 로그인만 사용하므로 패스워드는 null
        return null;
    }
    
    @Override
    public String getUsername() {
        // 이메일을 username으로 사용
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
        // OAuth2User 인터페이스 메서드
        return member.getNaverId();
    }
    
    public Long getMemberId() {
        return member.getId();
    }
    
    public String getEmail() {
        return member.getEmail();
    }
    
    public String getMemberName() {
        return member.getName();
    }

    public String getPhoneNumber() {
        return member.getPhoneNumber();
    }

    public LocalDate getBirthdate() {
        return member.getBirthdate();
    }
}
