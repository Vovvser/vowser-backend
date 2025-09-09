package com.vowser.backend.application.service.speech;

import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
import com.vowser.backend.common.constants.ApiConstants;
import com.vowser.backend.common.enums.SpeechMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.EnumSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechProcessingService {

    private final SpeechService speechService;
    private final SpeechModeService speechModeService;
    private final McpIntegrationService mcpIntegrationService;

    public ResponseEntity<SpeechResponse> processVoiceCommand(SpeechTranscribeRequest request) {
        
        log.info("음성 처리 요청 시작: sessionId=[{}], fileSize=[{}KB]", 
                request.getSessionId(), request.getAudioFile().getSize() / 1024);

        boolean hasSpecialModes = request.isEnableNumberMode();
        boolean hasCustomPhrases = request.getCustomPhrases() != null && !request.getCustomPhrases().isEmpty();
        
        String transcript;
        if (!hasSpecialModes && !hasCustomPhrases) {
            transcript = speechService.transcribe(request.getAudioFile());
            log.info("기본 음성 인식 완료: sessionId=[{}], transcript=[{}]", request.getSessionId(), transcript);
        } else {
            EnumSet<SpeechMode> modes = speechModeService.buildModes(
                request.isEnableGeneralMode(), request.isEnableNumberMode(), 
                request.isEnableAlphabetMode(), request.isEnableSnippetMode());
            transcript = speechService.transcribeWithModes(request.getAudioFile(), modes, request.getCustomPhrases());
            log.info("모드별 음성 인식 완료: sessionId=[{}], transcript=[{}]", request.getSessionId(), transcript);
        }

        try {
            mcpIntegrationService.sendVoiceCommand(transcript, request.getSessionId());
            
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

    public ResponseEntity<Object> getMcpConnectionStatus() {
        boolean connected = mcpIntegrationService.isConnected();
        log.debug("MCP 서버 연결 상태 확인: {}", connected);
        
        return ResponseEntity.ok(java.util.Map.of(ApiConstants.RESPONSE_KEY_CONNECTED, connected));
    }
}