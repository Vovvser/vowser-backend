package com.vowser.backend.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class ControlService {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val objectMapper = jacksonObjectMapper()

    fun registerSession(session: WebSocketSession) {
        sessions[session.id] = session
        println("세션 등록: ${session.id}")
    }

    fun unregisterSession(session: WebSocketSession) {
        sessions.remove(session.id)
        println("세션 제거: ${session.id}")
    }

    // 실제 프로덕션에서는 특정 사용자 세션을 찾아 보내야 하지만, 여기서는 가장 최근 연결된 세션에 보낸다
    fun sendCommandToClient(command: Map<String, Any>) {
        val session = sessions.values.lastOrNull() // 가장 마지막에 연결된 세션
        session?.let {
            if (it.isOpen) {
                val jsonCommand = objectMapper.writeValueAsString(command)
                it.sendMessage(TextMessage(jsonCommand))
            }
        }
    }

    // MCP 서버에서 받은 메시지를 클라이언트로 중계
    fun relayMcpResponse(messageJson: String) {
        val session = sessions.values.lastOrNull()
        session?.let {
            if (it.isOpen) {
                try {
                    it.sendMessage(TextMessage(messageJson))
                    println("MCP 서버 응답을 클라이언트로 중계 완료")
                } catch (e: Exception) {
                    println("MCP 응답 중계 실패: ${e.message}")
                }
            } else {
                println("클라이언트 WebSocket 세션이 닫혀있습니다")
            }
        } ?: println("연결된 클라이언트 세션이 없습니다")
    }
}