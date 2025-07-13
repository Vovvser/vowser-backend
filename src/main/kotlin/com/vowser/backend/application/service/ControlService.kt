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
}