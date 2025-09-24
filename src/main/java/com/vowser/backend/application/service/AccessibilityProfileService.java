package com.vowser.backend.application.service;

import com.vowser.backend.common.enums.DisabilityType;
import com.vowser.backend.common.exception.BusinessException;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.domain.member.entity.AccessibilityProfile;
import com.vowser.backend.domain.member.entity.Member;
import com.vowser.backend.domain.member.repository.AccessibilityProfileRepository;
import com.vowser.backend.domain.member.repository.MemberRepository;
import com.vowser.backend.infrastructure.crypto.StringCryptoConverter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessibilityProfileService {

    private final AccessibilityProfileRepository accessibilityProfileRepository;
    private final MemberRepository memberRepository;
    private final StringCryptoConverter cryptoConverter;

    @Transactional(readOnly = true)
    public Optional<AccessibilityProfile> findProfileByMemberId(Long memberId) {
        return accessibilityProfileRepository.findByMemberId(memberId);
    }

    @Transactional
    public AccessibilityProfile createOrUpdateProfile(Long memberId, DisabilityType type, String settingsJson) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        AccessibilityProfile profile = accessibilityProfileRepository.findByMemberId(memberId)
                .orElseGet(() -> AccessibilityProfile.builder().member(member).build());

        if (!profile.canBeApplied()) {
            profile.agreeConsent(LocalDateTime.now());
        }

        String encryptedSettings = cryptoConverter.encrypt(settingsJson);
        profile.updateProfile(type, encryptedSettings);

        return accessibilityProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(Long memberId) {
        AccessibilityProfile profile = accessibilityProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for member id: " + memberId));
        accessibilityProfileRepository.delete(profile);
    }
}