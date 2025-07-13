package com.vowser.backend.infrastructure.control

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vowser.backend.api.dto.CallToolRequest
import com.vowser.backend.application.service.ControlService
import com.vowser.backend.infrastructure.control.tool.BrowserTool
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class ControlWebSocketHandler(
    private val controlService: ControlService,
    private val toolRegistry: ToolRegistry,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        controlService.registerSession(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val request: CallToolRequest = objectMapper.readValue(message.payload)

            val tool: BrowserTool<*> = toolRegistry.getTool(request.toolName)
                ?: throw RuntimeException("Tool을 찾을 수 없습니다: ${request.toolName}")

            val toolArgs: Any = objectMapper.convertValue(request.args, tool.argumentType)

            @Suppress("UNCHECKED_CAST")
            val unsafeTool = tool as BrowserTool<Any>
            val result = unsafeTool.execute(toolArgs)

            session.sendMessage(TextMessage(objectMapper.writeValueAsString(result)))

        } catch (e: Exception) {
            println("메시지 처리 중 에러 발생: ${e.message}")
            // 에러 처리 로직
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        controlService.unregisterSession(session)
    }
}