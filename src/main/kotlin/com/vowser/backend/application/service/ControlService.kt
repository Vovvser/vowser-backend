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
                val allPathsResponse = convertMcpToAllPathsResponse(mcpResponse)
                if (allPathsResponse != null && allPathsResponse.paths.isNotEmpty()) {
                    val command = mapOf(
                        "type" to "all_navigation_paths",
                        "data" to allPathsResponse
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

    /**
     * MCP 응답을 AllPathsResponse DTO로 변환하는 함수
     */
    private fun convertMcpToAllPathsResponse(mcpResult: McpSearchPathResult): AllPathsResponse? {
        if (mcpResult.status != "success" || mcpResult.data.matched_paths.isNullOrEmpty()) {
            println("변환 실패: status=${mcpResult.status}, paths=${mcpResult.data.matched_paths?.size ?: 0}")
            return null
        }

        // example.com 테스트 경로 필터링
        val validPaths = mcpResult.data.matched_paths.filter { path ->
            !path.steps.firstOrNull()?.url.orEmpty().contains("example.com", ignoreCase = true)
        }

        if (validPaths.isEmpty()) {
            println("변환 실패: 모든 경로가 example.com 테스트 경로입니다.")
            return null
        }

        val pathDetails = validPaths.map { mcpPath ->
            val navigationSteps = mcpPath.steps.map { mcpStep ->
                val clientAction = determineClientAction(mcpStep)
                NavigationStep(
                    url = mcpStep.url,
                    title = mcpStep.title,
                    action = clientAction,
                    selector = mcpStep.selector,
                    htmlAttributes = if (clientAction == "type") {
                        extractInputValueFromAction(mcpStep.action)?.let { mapOf("value" to it) }
                    } else null
                )
            }

            PathDetail(
                pathId = mcpPath.pathId,
                score = mcpPath.score,
                total_weight = mcpPath.total_weight,
                last_used = mcpPath.last_used,
                estimated_time = mcpPath.estimated_time,
                steps = navigationSteps
            )
        }

        return AllPathsResponse(
            query = mcpResult.data.query,
            paths = pathDetails
        )
    }

    /**
     * [헬퍼 함수] McpStep을 기반으로 클라이언트에서 사용할 action 타입을 결정
     */
    private fun determineClientAction(mcpStep: McpStep): String {
        return when {
            mcpStep.action.contains("navigate", ignoreCase = true) ||
                    mcpStep.action.contains("이동", ignoreCase = true) ||
                    mcpStep.action.contains("접속", ignoreCase = true) -> "navigate"

            mcpStep.action.contains("click", ignoreCase = true) ||
                    mcpStep.action.contains("클릭", ignoreCase = true) -> "click"

            mcpStep.action.contains("type", ignoreCase = true) ||
                    mcpStep.action.contains("입력", ignoreCase = true) -> "type"

            // 명시적 action이 없을 때의 추론 규칙
            mcpStep.selector.isNullOrBlank() -> "navigate"
            !mcpStep.selector.isNullOrBlank() -> "click"
            else -> "navigate" // 기본값
        }
    }

    /**
     * [헬퍼 함수] action 문자열에서 'type' 액션에 필요한 입력값을 추출
     */
    private fun extractInputValueFromAction(action: String): String? {
        val patterns = listOf(
            """입력.*?[:：]\s*(.+)""".toRegex(),
            """type.*?[:：]\s*(.+)""".toRegex(RegexOption.IGNORE_CASE),
            """\"(.+)\"\s*입력""".toRegex(),
            """'(.+)'\s*입력""".toRegex()
        )
        patterns.forEach { pattern ->
            pattern.find(action)?.let { matchResult ->
                return matchResult.groupValues[1].trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }
}