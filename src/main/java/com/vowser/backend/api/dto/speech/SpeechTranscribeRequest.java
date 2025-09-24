package com.vowser.backend.api.dto.speech;

import com.vowser.backend.api.dto.member.AccessibilityProfileDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 음성 인식 및 처리 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechTranscribeRequest {

    @Schema(description = "인식할 음성 파일 (WAV, MP3, FLAC 등 지원)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "음성 파일은 필수입니다")
    private MultipartFile audioFile;

    @Schema(
            description = "세션 식별자 (클라이언트별 고유 ID)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "user-session-12345"
    )
    @NotBlank(message = "세션 ID는 필수입니다")
    private String sessionId;

    @Schema(description = "로그인하지 않은 사용자를 위한 임시 접근성 설정 (세션 맞춤 모드)")
    private AccessibilityProfileDto.CreateOrUpdateRequest accessibilityContext;

    @Schema(description = "일반 음성 인식 모드 활성화", example = "true")
    private boolean enableGeneralMode = true;

    @Schema(description = "숫자 인식 최적화 모드 활성화", example = "false")
    private boolean enableNumberMode = false;

    @Schema(description = "알파벳 인식 최적화 모드 활성화", example = "false")
    private boolean enableAlphabetMode = false;

    @Schema(description = "코드/명령어 인식 모드 활성화", example = "false")
    private boolean enableSnippetMode = false;

    @Schema(description = "추가 커스텀 phrase hints")
    private List<String> customPhrases;
}