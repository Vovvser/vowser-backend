package com.vowser.backend.api.dto.member;

import com.vowser.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    
    /**
     * 회원 ID
     */
    private Long id;
    
    /**
     * 이메일
     */
    private String email;
    
    /**
     * 이름
     */
    private String name;
    
    /**
     * 네이버 ID
     */
    private String naverId;
    
    /**
     * 가입일시
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;
    
    /**
     * Entity를 DTO로 변환
     */
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .naverId(member.getNaverId())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}