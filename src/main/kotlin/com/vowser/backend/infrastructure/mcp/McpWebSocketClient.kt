package com.vowser.backend.infrastructure.mcp

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vowser.backend.application.service.ControlService
import okhttp3.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Component
class McpWebSocketClient(
    private val controlService: ControlService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var isConnected = false

    @PostConstruct
    fun connect() {
        val request = Request.Builder()
            .url("ws://localhost:8000/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                logger.info("MCP 서버 연결 성공: ws://localhost:8000/ws")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                logger.info("MCP 서버에서 메시지 수신: {}", text)
                controlService.relayMcpResponse(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                logger.error("MCP 서버 연결 실패: {}", t.message)
                
                Thread.sleep(5000)
                reconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                logger.warn("MCP 서버 연결 종료 중: {} - {}", code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                logger.warn("MCP 서버 연결 종료됨: {} - {}", code, reason)
            }
        })
    }

    private fun reconnect() {
        logger.info("MCP 서버 재연결 시도...")
        connect()
    }

    fun sendVoiceCommand(transcript: String, sessionId: String) {
        if (!isConnected) {
            logger.error("MCP 서버에 연결되어 있지 않습니다. 메시지 전송 실패")
            return
        }

        val message = mapOf(
            "type" to "process_voice_command",
            "data" to mapOf(
                "transcript" to transcript,
                "sessionId" to sessionId
            )
        )

        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            val success = webSocket?.send(jsonMessage) ?: false
            
            if (success) {
                logger.info("MCP 서버로 음성 명령 전송 성공: sessionId={}, transcript={}", sessionId, transcript)
            } else {
                logger.error("MCP 서버로 메시지 전송 실패")
            }
        } catch (e: Exception) {
            logger.error("JSON 직렬화 실패: {}", e.message)
        }
    }

    @PreDestroy
    fun disconnect() {
        webSocket?.close(1000, "Application shutdown")
        client.dispatcher.executorService.shutdown()
        logger.info("MCP 서버 연결 종료")
    }

    fun isConnected(): Boolean = isConnected
}