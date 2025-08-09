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
        if (mcpResult.status != "success" || mcpResult.data.matched_paths?.isEmpty() != false) {
            println("변환 실패: status=${mcpResult.status}, paths=${mcpResult.data.matched_paths?.size ?: 0}, message=${mcpResult.data.message}")
            return null
        }

        // example.com 테스트 경로 필터링 후 최적 경로 선택
        val validPaths = mcpResult.data.matched_paths!!.filter { path ->
            val startUrl = path.steps.firstOrNull()?.url?.lowercase() ?: ""
            !startUrl.contains("example.com")
        }
        
        if (validPaths.isEmpty()) {
            println("변환 실패: 모든 경로가 example.com 테스트 경로입니다.")
            return null
        }
        
        val bestPath = validPaths.maxByOrNull { it.score ?: 0.0 }!!
        println("최적 경로 변환 시작: pathId=${bestPath.pathId}, score=${bestPath.score}, steps=${bestPath.steps.size}개 (example.com 제외 후 ${validPaths.size}/${mcpResult.data.matched_paths!!.size}개 경로 중 선택)")

        val navigationSteps = bestPath.steps
            .mapIndexed { index, step -> step to index } // 순서 보장을 위해 인덱스 사용
            .sortedBy { it.second }
            .map { it.first }
            .map { mcpStep ->
                // MCP의 action 필드를 우선으로 하고, 없으면 type과 selector로 판단
                val clientAction = when {
                    // MCP action 필드가 명확히 navigate 관련인 경우
                    mcpStep.action.contains("접속", ignoreCase = true) || 
                    mcpStep.action.contains("이동", ignoreCase = true) ||
                    mcpStep.action.contains("navigate", ignoreCase = true) -> "navigate"
                    
                    // MCP action 필드가 명확히 click 관련인 경우
                    mcpStep.action.contains("클릭", ignoreCase = true) ||
                    mcpStep.action.contains("click", ignoreCase = true) -> "click"
                    
                    // MCP action 필드가 type 관련인 경우
                    mcpStep.action.contains("입력", ignoreCase = true) ||
                    mcpStep.action.contains("type", ignoreCase = true) -> "type"
                    
                    // URL만 있고 selector가 없으면 navigate
                    mcpStep.selector.isNullOrBlank() -> "navigate"
                    
                    // selector가 있으면 click (대부분의 상호작용)
                    !mcpStep.selector.isNullOrBlank() -> "click"
                    
                    // 기본값은 navigate
                    else -> "navigate"
                }

                NavigationStep(
                    url = mcpStep.url,
                    title = mcpStep.title,
                    action = clientAction,
                    selector = mcpStep.selector,
                    htmlAttributes = when (clientAction) {
                        "type" -> {
                            // textLabels에서 입력값을 추출하거나, action에서 파싱 시도
                            val inputValue = extractInputValueFromAction(mcpStep.action) 
                                ?: ""
                            if (inputValue.isNotEmpty()) mapOf("value" to inputValue) else null
                        }
                        else -> null
                    }
                )
            }

        return NavigationPathRequest(
            pathId = bestPath.pathId,
            description = "MCP Query: ${mcpResult.data.query}",
            steps = navigationSteps
        )
    }

    private fun extractInputValueFromAction(action: String): String? {
        // "구글 입력", "검색어 입력: 구글", "type: 구글" 등에서 입력값 추출
        val patterns = listOf(
            """입력.*?[:：]\s*(.+)""".toRegex(),
            """type.*?[:：]\s*(.+)""".toRegex(RegexOption.IGNORE_CASE),
            """\"(.+)\"\s*입력""".toRegex(),
            """'(.+)'\s*입력""".toRegex()
        )
        
        patterns.forEach { pattern ->
            pattern.find(action)?.let { matchResult ->
                val value = matchResult.groupValues[1].trim()
                if (value.isNotEmpty()) return value
            }
        }
        
        return null
    }
}