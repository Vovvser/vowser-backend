package com.vowser.backend.api.controller

import com.vowser.backend.api.dto.SpeechResponse
import com.vowser.backend.application.service.ControlService
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
    private val controlService: ControlService,
    private val mcpWebSocketClient: McpWebSocketClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/transcribe")
    fun transcribeAndExecute(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("sessionId") sessionId: String // 어떤 클라이언트의 요청인지 식별
    ): ResponseEntity<SpeechResponse> {
        return try {
            // 1. SpeechService를 통해 음성을 텍스트로 변환
            val transcript = speechService.transcribe(audioFile)
            logger.info("음성 인식 완료: sessionId=[${sessionId}], transcript=[${transcript}]")

            // 2. MCP 서버로 음성 명령 전달 (중계자 역할)
            if (mcpWebSocketClient.isConnected()) {
                mcpWebSocketClient.sendVoiceCommand(transcript, sessionId)
                logger.info("MCP 서버로 음성 명령 전송 완료")
            } else {
                logger.warn("MCP 서버 연결 없음. Mock 데이터로 테스트 진행")
                // Mock 그래프 데이터를 클라이언트로 전송 (테스트용)
                sendMockGraphData(transcript, sessionId)
            }

            ResponseEntity.ok(SpeechResponse(success = true, transcript = transcript))
        } catch (e: Exception) {
            logger.error("음성 처리 중 오류 발생", e)
            ResponseEntity.internalServerError().body(SpeechResponse(success = false, transcript = null, message = e.message))
        }
    }

    private fun sendMockGraphData(transcript: String, sessionId: String) {
        // 클라이언트가 기대하는 형태의 Mock 그래프 데이터 생성
        val mockGraphData = when {
            transcript.contains("네이버", ignoreCase = true) -> createNaverMockData(transcript, sessionId)
            transcript.contains("구글", ignoreCase = true) -> createGoogleMockData(transcript, sessionId)
            transcript.contains("검색", ignoreCase = true) -> createSearchMockData(transcript, sessionId)
            else -> createDefaultMockData(transcript, sessionId)
        }

        logger.info("Mock 그래프 데이터 전송: {}", mockGraphData)
        controlService.relayMcpResponse(mockGraphData)
    }

    private fun createNaverMockData(transcript: String, sessionId: String): String {
        return """
        {
            "type": "graph_update",
            "status": "success",
            "data": {
                "sessionId": "$sessionId",
                "nodes": [
                    {
                        "id": "voice_start",
                        "label": "음성 명령",
                        "type": "VOICE_START",
                        "description": "사용자 음성 입력",
                        "position": {"x": 100, "y": 100}
                    },
                    {
                        "id": "browser_action",
                        "label": "브라우저 실행",
                        "type": "ACTION",
                        "description": "브라우저 탭 활성화",
                        "position": {"x": 300, "y": 100}
                    },
                    {
                        "id": "naver_main",
                        "label": "네이버 메인",
                        "type": "WEBSITE",
                        "url": "https://www.naver.com",
                        "description": "네이버 메인 페이지",
                        "keywords": ["포털", "검색", "뉴스"],
                        "position": {"x": 500, "y": 100}
                    },
                    {
                        "id": "naver_loaded",
                        "label": "페이지 로딩 완료",
                        "type": "RESULT",
                        "description": "네이버 메인 페이지 로딩 완료",
                        "position": {"x": 700, "y": 100}
                    }
                ],
                "edges": [
                    {
                        "id": "e1",
                        "source": "voice_start",
                        "target": "browser_action",
                        "type": "EXECUTES",
                        "label": "명령 실행"
                    },
                    {
                        "id": "e2",
                        "source": "browser_action",
                        "target": "naver_main",
                        "type": "NAVIGATES_TO",
                        "label": "페이지 이동"
                    },
                    {
                        "id": "e3",
                        "source": "naver_main",
                        "target": "naver_loaded",
                        "type": "LEADS_TO",
                        "label": "로딩 완료"
                    }
                ],
                "highlightedPath": ["voice_start", "browser_action", "naver_main", "naver_loaded"],
                "activeNodeId": "naver_loaded",
                "metadata": {
                    "voiceCommand": "$transcript",
                    "timestamp": "${java.time.LocalDateTime.now()}",
                    "processingTime": 1500
                }
            }
        }
        """.trimIndent()
    }

    private fun createGoogleMockData(transcript: String, sessionId: String): String {
        return """
        {
            "type": "graph_update",
            "status": "success",
            "data": {
                "sessionId": "$sessionId",
                "nodes": [
                    {
                        "id": "voice_start",
                        "label": "음성 명령",
                        "type": "VOICE_START",
                        "position": {"x": 100, "y": 100}
                    },
                    {
                        "id": "google_main",
                        "label": "구글 메인",
                        "type": "WEBSITE",
                        "url": "https://www.google.com",
                        "position": {"x": 400, "y": 100}
                    },
                    {
                        "id": "search_ready",
                        "label": "검색 준비",
                        "type": "RESULT",
                        "position": {"x": 700, "y": 100}
                    }
                ],
                "edges": [
                    {
                        "id": "e1",
                        "source": "voice_start",
                        "target": "google_main",
                        "type": "NAVIGATES_TO",
                        "label": "구글 이동"
                    },
                    {
                        "id": "e2",
                        "source": "google_main",
                        "target": "search_ready",
                        "type": "LEADS_TO",
                        "label": "검색 준비"
                    }
                ],
                "highlightedPath": ["voice_start", "google_main", "search_ready"],
                "activeNodeId": "search_ready",
                "metadata": {
                    "voiceCommand": "$transcript",
                    "timestamp": "${java.time.LocalDateTime.now()}",
                    "processingTime": 1200
                }
            }
        }
        """.trimIndent()
    }

    private fun createSearchMockData(transcript: String, sessionId: String): String {
        return """
        {
            "type": "graph_update",
            "status": "success",
            "data": {
                "sessionId": "$sessionId",
                "nodes": [
                    {
                        "id": "voice_start",
                        "label": "음성 명령",
                        "type": "VOICE_START",
                        "position": {"x": 100, "y": 100}
                    },
                    {
                        "id": "search_input",
                        "label": "검색어 입력",
                        "type": "ACTION",
                        "position": {"x": 300, "y": 100}
                    },
                    {
                        "id": "search_execute",
                        "label": "검색 실행",
                        "type": "ACTION",
                        "position": {"x": 500, "y": 100}
                    },
                    {
                        "id": "search_results",
                        "label": "검색 결과",
                        "type": "PAGE",
                        "position": {"x": 700, "y": 100}
                    }
                ],
                "edges": [
                    {
                        "id": "e1",
                        "source": "voice_start",
                        "target": "search_input",
                        "type": "EXECUTES",
                        "label": "검색어 입력"
                    },
                    {
                        "id": "e2",
                        "source": "search_input",
                        "target": "search_execute",
                        "type": "LEADS_TO",
                        "label": "검색 실행"
                    },
                    {
                        "id": "e3",
                        "source": "search_execute",
                        "target": "search_results",
                        "type": "NAVIGATES_TO",
                        "label": "결과 표시"
                    }
                ],
                "highlightedPath": ["voice_start", "search_input", "search_execute", "search_results"],
                "activeNodeId": "search_results",
                "metadata": {
                    "voiceCommand": "$transcript",
                    "timestamp": "${java.time.LocalDateTime.now()}",
                    "processingTime": 2000
                }
            }
        }
        """.trimIndent()
    }

    private fun createDefaultMockData(transcript: String, sessionId: String): String {
        return """
        {
            "type": "graph_update",
            "status": "success",
            "data": {
                "sessionId": "$sessionId",
                "nodes": [
                    {
                        "id": "voice_start",
                        "label": "음성 명령",
                        "type": "VOICE_START",
                        "position": {"x": 100, "y": 100}
                    },
                    {
                        "id": "command_processing",
                        "label": "명령 처리",
                        "type": "ACTION",
                        "position": {"x": 400, "y": 100}
                    },
                    {
                        "id": "processing_complete",
                        "label": "처리 완료",
                        "type": "RESULT",
                        "position": {"x": 700, "y": 100}
                    }
                ],
                "edges": [
                    {
                        "id": "e1",
                        "source": "voice_start",
                        "target": "command_processing",
                        "type": "EXECUTES",
                        "label": "명령 분석"
                    },
                    {
                        "id": "e2",
                        "source": "command_processing",
                        "target": "processing_complete",
                        "type": "LEADS_TO",
                        "label": "완료"
                    }
                ],
                "highlightedPath": ["voice_start", "command_processing", "processing_complete"],
                "activeNodeId": "processing_complete",
                "metadata": {
                    "voiceCommand": "$transcript",
                    "timestamp": "${java.time.LocalDateTime.now()}",
                    "processingTime": 1000
                }
            }
        }
        """.trimIndent()
    }
}