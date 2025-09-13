package com.vowser.backend.api.controller;

import com.vowser.backend.api.doc.SpeechApiDocument;
import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
import com.vowser.backend.application.service.speech.SpeechProcessingService;
import com.vowser.backend.common.constants.ApiConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성 처리 컨트롤러
 *
 * 음성을 텍스트로 변환하는 작업과 음성 명령 처리를 담당합니다.
 * Google Cloud Speech-to-Text API와 연동하여 변환된 명령을
 * MCP 서버로 전달합니다.
 */
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
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "enableGeneralMode", defaultValue = "true") boolean enableGeneralMode,
            @RequestParam(value = "enableNumberMode", defaultValue = "false") boolean enableNumberMode,
            @RequestParam(value = "enableAlphabetMode", defaultValue = "false") boolean enableAlphabetMode,
            @RequestParam(value = "enableSnippetMode", defaultValue = "false") boolean enableSnippetMode,
            @RequestParam(required = false) java.util.List<String> customPhrases) {
        
        SpeechTranscribeRequest request = new SpeechTranscribeRequest(
            audioFile, sessionId,
            enableGeneralMode, enableNumberMode,
            enableAlphabetMode, enableSnippetMode, customPhrases
        );
        
        return speechProcessingService.processVoiceCommand(request);
    }


    @SpeechApiDocument.McpStatus
    @GetMapping("/mcp-status")
    public ResponseEntity<Object> getMcpConnectionStatus() {
        return speechProcessingService.getMcpConnectionStatus();
    }
}