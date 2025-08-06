package com.vowser.backend.api.controller

import com.vowser.backend.api.dto.SpeechResponse
import com.vowser.backend.application.service.SpeechService
import com.vowser.backend.infrastructure.mcp.McpWebSocketClient
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/speech")
class SpeechController(
    private val speechService: SpeechService,
    private val mcpWebSocketClient: McpWebSocketClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/transcribe")
    fun transcribeAndExecute(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("sessionId") sessionId: String
    ): ResponseEntity<SpeechResponse> {
        return try {
            val transcript = speechService.transcribe(audioFile)
            logger.info("음성 인식 완료: sessionId=[${sessionId}], transcript=[${transcript}]")

            if (mcpWebSocketClient.isConnected()) {
                mcpWebSocketClient.sendVoiceCommand(transcript, sessionId)
                logger.info("MCP 서버로 음성 명령 전송 완료")
            } else {
                logger.error("MCP 서버에 연결되지 않음")
                return ResponseEntity.internalServerError().body(
                    SpeechResponse(success = false, transcript = transcript, message = "MCP 서버에 연결되지 않음")
                )
            }

            ResponseEntity.ok(SpeechResponse(success = true, transcript = transcript))
        } catch (e: Exception) {
            logger.error("음성 처리 중 오류 발생", e)
            ResponseEntity.internalServerError().body(SpeechResponse(success = false, transcript = null, message = e.message))
        }
    }

}