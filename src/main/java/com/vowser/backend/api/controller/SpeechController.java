package com.vowser.backend.api.controller;

import com.vowser.backend.api.doc.SpeechApiDocument;
import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.application.service.speech.SpeechService;
import com.vowser.backend.common.constants.ApiConstants;
import com.vowser.backend.common.constants.NetworkConstants;
import com.vowser.backend.infrastructure.mcp.McpWebSocketClient;
import io.swagger.v3.oas.annotations.Parameter;
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

    private final SpeechService speechService;
    private final McpWebSocketClient mcpWebSocketClient;

    @SpeechApiDocument.TranscribeAndExecute
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpeechResponse> transcribeAndExecute(
            @Parameter(
                description = "인식할 음성 파일 (WAV, MP3, FLAC 등 지원)",
                required = true
            )
            @RequestParam("audio") MultipartFile audioFile,
            
            @Parameter(
                description = "세션 식별자 (클라이언트별 고유 ID)",
                required = true,
                example = "user-session-12345"
            )
            @RequestParam("sessionId") String sessionId) {
        
        try {
            log.info("음성 인식 요청 수신: sessionId=[{}], fileSize=[{}KB]", 
                    sessionId, audioFile.getSize() / NetworkConstants.DataSize.BYTES_PER_KB);
            
            String transcript = speechService.transcribe(audioFile);
            log.info("음성 인식 완료: sessionId=[{}], transcript=[{}]", sessionId, transcript);

            if (mcpWebSocketClient.isConnected()) {
                mcpWebSocketClient.sendVoiceCommand(transcript, sessionId);
                log.info("MCP 서버로 음성 명령 전송 완료: sessionId=[{}]", sessionId);
                
                return ResponseEntity.ok(
                    new SpeechResponse(true, transcript, null)
                );
            } else {
                log.error("MCP 서버에 연결되지 않음: sessionId=[{}]", sessionId);
                return ResponseEntity.internalServerError().body(
                    new SpeechResponse(false, transcript, "MCP 서버에 연결되지 않음")
                );
            }

        } catch (Exception e) {
            log.error("음성 처리 중 오류 발생: sessionId=[{}]", sessionId, e);
            return ResponseEntity.internalServerError().body(
                new SpeechResponse(false, null, e.getMessage())
            );
        }
    }


    @SpeechApiDocument.McpStatus
    @GetMapping("/mcp-status")
    public ResponseEntity<Object> getMcpConnectionStatus() {
        boolean connected = mcpWebSocketClient.isConnected();
        log.debug("MCP 서버 연결 상태 확인: {}", connected);
        
        return ResponseEntity.ok(java.util.Map.of(ApiConstants.RESPONSE_KEY_CONNECTED, connected));
    }
}