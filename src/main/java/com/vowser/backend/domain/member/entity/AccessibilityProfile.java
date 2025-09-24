package com.vowser.backend.domain.member.entity;

import com.vowser.backend.common.entity.BaseEntity;
import com.vowser.backend.common.enums.DisabilityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accessibility_profiles", indexes = {
        @Index(name = "uk_profile_member", columnList = "member_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class AccessibilityProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_profile_member"))
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "disability_type", nullable = false, length = 50)
    private DisabilityType disabilityType;

    @Lob
    @Column(name = "profile_settings")
    private String profileSettings;

    /** 개인정보 활용 동의 */
    @Column(name = "consent_agreed_at")
    private LocalDateTime consentAgreedAt;

    /** 활용 가능 여부 */
    public boolean canBeApplied() {
        return consentAgreedAt != null;
    }

    /** 동의 처리 */
    public void agreeConsent(LocalDateTime now) {
        this.consentAgreedAt = now;
    }

    /** 장애 유형/설정 변경 */
    public void updateProfile(
            DisabilityType type,
            String settingsJson
    ) {
        if (type != null) this.disabilityType = type;
        this.profileSettings = settingsJson;
    }
}

