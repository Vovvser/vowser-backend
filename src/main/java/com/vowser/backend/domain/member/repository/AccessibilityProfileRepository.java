package com.vowser.backend.domain.member.repository;

import com.vowser.backend.domain.member.entity.AccessibilityProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessibilityProfileRepository extends JpaRepository<AccessibilityProfile, Long> {
    Optional<AccessibilityProfile> findByMemberId(Long memberId);
    boolean existsByMemberId(Long memberId);
}
