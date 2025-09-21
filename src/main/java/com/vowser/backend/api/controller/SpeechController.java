package com.vowser.backend.api.controller;

import com.vowser.backend.api.doc.SpeechApiDocument;
import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
import com.vowser.backend.application.service.speech.SpeechProcessingService;
import com.vowser.backend.common.constants.ApiConstants;
import com.vowser.backend.infrastructure.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Speech Processing", description = "음성 인식 및 처리 API")
@RestController
@RequestMapping(ApiConstants.API_PATH_SPEECH)
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechProcessingService speechProcessingService;

    @SpeechApiDocument.TranscribeAndExecute
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpeechResponse> transcribeAndExecute(
            @Valid @ModelAttribute SpeechTranscribeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // userDetails는 JWT 토큰이 유효하면 주입되고, 없으면 null이 됨
        return speechProcessingService.processVoiceCommand(request, userDetails);
    }

    @SpeechApiDocument.McpStatus
    @GetMapping("/mcp-status")
    public ResponseEntity<Object> getMcpConnectionStatus() {
        return speechProcessingService.getMcpConnectionStatus();
    }
}
