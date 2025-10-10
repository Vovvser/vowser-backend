package com.vowser.backend.domain.member.entity;

import com.vowser.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 회원 엔티티 (네이버 소셜 로그인 전용)
 */
@Entity
@Table(name = "members", indexes = {
    @Index(name = "idx_member_email", columnList = "email", unique = true),
    @Index(name = "idx_member_naver_id", columnList = "naver_id", unique = true),
    @Index(name = "idx_member_phone", columnList = "phone_number", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(name = "naver_id", nullable = false, unique = true, length = 100)
    private String naverId;  // 네이버에서 제공하는 고유 ID

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;  // 휴대폰 번호

    @Column(name = "birthdate", nullable = false)
    private LocalDate birthdate;  // 생년월일

    @Builder
    private Member(String email, String name, String naverId, String phoneNumber, LocalDate birthdate) {
        this.email = email;
        this.name = name;
        this.naverId = naverId;
        this.phoneNumber = phoneNumber;
        this.birthdate = birthdate;
    }
    
    /**
     * 네이버 OAuth2 정보로 회원 생성 (자동 회원가입)
     */
    public static Member createNaverMember(String email, String name, String naverId, String phoneNumber, LocalDate birthdate) {
        return Member.builder()
                .email(email)
                .name(name)
                .naverId(naverId)
                .phoneNumber(phoneNumber)
                .birthdate(birthdate)
                .build();
    }
    
    /**
     * 프로필 이름 업데이트
     */
    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }
}
