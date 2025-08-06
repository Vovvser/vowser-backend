package com.vowser.backend.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vowser.backend.api.dto.*
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

    fun sendCommandToClient(command: Map<String, Any>) {
        val session = sessions.values.lastOrNull() // 가장 마지막에 연결된 세션
        if (session == null || !session.isOpen) {
            println("명령 전송 실패: 연결된 클라이언트 세션이 없거나 닫혀있습니다.")
            return
        }

        try {
            val jsonCommand = objectMapper.writeValueAsString(command)
            session.sendMessage(TextMessage(jsonCommand))
            println("클라이언트로 명령 전송 완료: $jsonCommand")
        } catch (e: Exception) {
            println("명령 전송 중 오류 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    // MCP 서버에서 받은 메시지를 클라이언트로 중계
    fun relayMcpResponse(messageJson: String) {
        val session = sessions.values.lastOrNull()
        if (session == null || !session.isOpen) {
            println("연결된 클라이언트 세션이 없거나 닫혀있습니다.")
            return
        }

        try {
            val mcpResponse = objectMapper.readValue<McpSearchPathResult>(messageJson)

            if (mcpResponse.type == "search_path_result") {
                val navigationPath = convertMcpToNavigationPath(mcpResponse)
                if (navigationPath != null) {
                    val command = mapOf(
                        "type" to "navigation_path",
                        "data" to navigationPath
                    )
                    val jsonCommand = objectMapper.writeValueAsString(command)
                    session.sendMessage(TextMessage(jsonCommand))
                    println("MCP 응답을 NavigationPath로 변환하여 클라이언트로 전송 완료")
                } else {
                    println("변환할 유효한 경로가 없어 클라이언트로 전송하지 않음")
                }
            } else {
                session.sendMessage(TextMessage(messageJson))
                println("MCP 서버 응답(type: ${mcpResponse.type})을 클라이언트로 그대로 중계 완료")
            }
        } catch (e: Exception) {
            println("MCP 응답 중계 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun convertMcpToNavigationPath(mcpResult: McpSearchPathResult): NavigationPathRequest? {
        // 성공 상태이고, 유효한 경로가 1개 이상 있을 때만 변환
        if (mcpResult.status != "success" || mcpResult.data.matched_paths.isEmpty()) {
            println("변환 실패: status=${mcpResult.status}, paths=${mcpResult.data.matched_paths.size}")
            return null
        }

        // 가장 관련도 높은 첫 번째 경로를 사용
        val firstPath = mcpResult.data.matched_paths.first()
        println("첫 번째 경로 변환 시작: pathId=${firstPath.pathId}, steps=${firstPath.steps.size}개")

        val navigationSteps = firstPath.steps
            .sortedBy { it.order } // 순서 보장
            .map { mcpStep ->
                // MCP의 action을 클라이언트의 action("navigate", "click")으로 변환
                val clientAction = when {
                    // 타입이 ROOT이거나, selector가 없으면 'navigate'로 간주
                    mcpStep.type == "ROOT" -> "navigate"
                    mcpStep.selector.isNullOrBlank() -> "navigate"
                    // selector가 있으면 'click'으로 간주
                    else -> "click"
                }

                NavigationStep(
                    url = mcpStep.url,
                    title = mcpStep.title,
                    action = clientAction,
                    selector = mcpStep.selector
                )
            }

        return NavigationPathRequest(
            pathId = firstPath.pathId,
            description = "MCP Query: ${mcpResult.data.query}",
            steps = navigationSteps
        )
    }
}