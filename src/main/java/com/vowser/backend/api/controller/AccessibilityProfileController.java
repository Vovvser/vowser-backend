package com.vowser.backend.api.controller;

import com.vowser.backend.api.dto.common.ApiResponse;
import com.vowser.backend.api.dto.member.AccessibilityProfileDto;
import com.vowser.backend.application.service.AccessibilityProfileService;
import com.vowser.backend.domain.member.entity.AccessibilityProfile;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import com.vowser.backend.infrastructure.crypto.StringCryptoConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Accessibility Profiles", description = "사용자 접근성 프로필 관리")
@RestController
@RequestMapping("/api/v1/member/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AccessibilityProfileController {

    private final AccessibilityProfileService accessibilityProfileService;
    private final StringCryptoConverter cryptoConverter;

    @Operation(summary = "내 접근성 프로필 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<AccessibilityProfileDto.ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AccessibilityProfile profile = accessibilityProfileService.findProfileByMemberId(userDetails.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("프로필을 찾을 수 없습니다."));
        
        String decryptedSettings = cryptoConverter.decrypt(profile.getProfileSettings());
        return ResponseEntity.ok(ApiResponse.success(new AccessibilityProfileDto.ProfileResponse(profile, decryptedSettings)));
    }

    @Operation(summary = "내 접근성 프로필 생성 또는 수정")
    @PostMapping
    public ResponseEntity<ApiResponse<AccessibilityProfileDto.ProfileResponse>> createOrUpdateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AccessibilityProfileDto.CreateOrUpdateRequest request) {

        AccessibilityProfile profile = accessibilityProfileService.createOrUpdateProfile(
                userDetails.getMemberId(),
                request.getDisabilityType(),
                request.getSettingsJson()
        );
        
        String decryptedSettings = cryptoConverter.decrypt(profile.getProfileSettings());
        return ResponseEntity.ok(ApiResponse.success(new AccessibilityProfileDto.ProfileResponse(profile, decryptedSettings)));
    }

    @Operation(summary = "내 접근성 프로필 삭제")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessibilityProfileService.deleteProfile(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
