package com.vowser.backend.application.service.speech;

import com.vowser.backend.api.dto.member.AccessibilityProfileDto;
import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
import com.vowser.backend.application.service.AccessibilityProfileService;
import com.vowser.backend.common.constants.ApiConstants;
import com.vowser.backend.common.enums.SpeechMode;
import com.vowser.backend.domain.member.entity.AccessibilityProfile;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechProcessingService {

    private final SpeechService speechService;
    private final SpeechModeService speechModeService;
    private final McpIntegrationService mcpIntegrationService;
    private final AccessibilityProfileService accessibilityProfileService;

    public ResponseEntity<SpeechResponse> processVoiceCommand(SpeechTranscribeRequest request, CustomUserDetails userDetails) {

        log.info("음성 처리 요청 시작: sessionId=[{}], fileSize=[{}KB]",
                request.getSessionId(), request.getAudioFile().getSize() / 1024);

        Optional<AccessibilityProfile> profileOptional = determineAccessibilityProfile(request, userDetails);

        profileOptional.ifPresent(profile ->
                log.info("접근성 프로필 적용: memberId=[{}], type=[{}]",
                        profile.getMember() != null ? profile.getMember().getId() : "session",
                        profile.getDisabilityType()));

        boolean hasSpecialModes = request.isEnableNumberMode();
        boolean hasCustomPhrases = request.getCustomPhrases() != null && !request.getCustomPhrases().isEmpty();

        String transcript;
        if (!hasSpecialModes && !hasCustomPhrases) {
            transcript = speechService.transcribe(request.getAudioFile());
            log.info("기본 음성 인식 완료: sessionId=[{}], transcript=[{}]", request.getSessionId(), transcript);
        } else {
            EnumSet<SpeechMode> modes = speechModeService.buildModes(
                    request.isEnableGeneralMode(), request.isEnableNumberMode(),
                    request.isEnableAlphabetMode());
            transcript = speechService.transcribeWithModes(request.getAudioFile(), modes, request.getCustomPhrases());
            log.info("모드별 음성 인식 완료: sessionId=[{}], transcript=[{}]", request.getSessionId(), transcript);
        }

        try {
            SpeechResponse response = SpeechResponse.builder()
                    .success(true)
                    .transcript(transcript)
                    .build();
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("MCP 연결 오류: sessionId=[{}]", request.getSessionId(), e);
            SpeechResponse response = SpeechResponse.builder()
                    .success(false)
                    .transcript(transcript)
                    .message("MCP 서버에 연결되지 않음")
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Optional<AccessibilityProfile> determineAccessibilityProfile(SpeechTranscribeRequest request, CustomUserDetails userDetails) {
        if (userDetails != null) {
            return accessibilityProfileService.findProfileByMemberId(userDetails.getMemberId());
        }
        if (request.getAccessibilityContext() != null) {
            AccessibilityProfileDto.CreateOrUpdateRequest tempContext = request.getAccessibilityContext();
            AccessibilityProfile tempProfile = AccessibilityProfile.builder()
                    .disabilityType(tempContext.getDisabilityType())
                    .profileSettings(tempContext.getSettingsJson()) // 암호화되지 않은 임시 설정
                    .build();
            return Optional.of(tempProfile);
        }
        return Optional.empty();
    }

    public ResponseEntity<Object> getMcpConnectionStatus() {
        boolean connected = mcpIntegrationService.isConnected();
        log.debug("MCP 서버 연결 상태 확인: {}", connected);

        return ResponseEntity.ok(java.util.Map.of(ApiConstants.RESPONSE_KEY_CONNECTED, connected));
    }
}
