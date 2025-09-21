package com.vowser.backend.api.dto.member;

import com.vowser.backend.common.enums.DisabilityType;
import com.vowser.backend.domain.member.entity.AccessibilityProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AccessibilityProfileDto {

    @Getter
    @NoArgsConstructor
    @Schema(title = "접근성 프로필 생성/수정 요청")
    public static class CreateOrUpdateRequest {
        @NotNull
        @Schema(description = "장애 유형", example = "VISUAL_IMPAIRMENT")
        private DisabilityType disabilityType;

        @Schema(description = "프로필 설정 (JSON 문자열)", example = "{\"screen_reader_mode\": \"FULL\"}")
        private String settingsJson;
    }

    @Getter
    @Schema(title = "접근성 프로필 응답")
    public static class ProfileResponse {
        @Schema(description = "프로필 ID")
        private final Long profileId;

        @Schema(description = "장애 유형")
        private final DisabilityType disabilityType;

        @Schema(description = "프로필 설정 (JSON 문자열)")
        private final String settingsJson;

        @Schema(description = "활용 동의 여부")
        private final boolean consentAgreed;

        public ProfileResponse(AccessibilityProfile profile, String decryptedSettingsJson) {
            this.profileId = profile.getId();
            this.disabilityType = profile.getDisabilityType();
            this.settingsJson = decryptedSettingsJson;
            this.consentAgreed = profile.canBeApplied();
        }
    }
}
