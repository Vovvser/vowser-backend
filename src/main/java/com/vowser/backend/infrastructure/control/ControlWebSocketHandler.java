package com.vowser.backend.infrastructure.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vowser.backend.api.dto.ControlDto;
import com.vowser.backend.application.service.ControlService;
import com.vowser.backend.application.service.speech.McpIntegrationService;
import com.vowser.backend.common.constants.ErrorMessages;
import com.vowser.backend.infrastructure.control.tool.BrowserTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

/**
 * 제어용 WebSocket 핸들러
 *
 * 브라우저 제어 작업을 위한 WebSocket 연결과 메시지를 처리
 * 클라이언트로부터의 툴 실행 요청을 처리하고 세션 생명주기를 관리
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class ControlWebSocketHandler extends TextWebSocketHandler {

    private final ControlService controlService;
    private final ToolRegistry toolRegistry;
    private final McpIntegrationService mcpIntegrationService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        controlService.registerSession(session);
        log.info("웹소켓 연결 설정 완료: sessionId=[{}], remoteAddress=[{}]", 
                session.getId(), session.getRemoteAddress());

        sendWelcomeMessage(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("웹소켓 메시지 수신: sessionId=[{}], messageLength=[{}]",
                session.getId(), payload.length());

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            log.debug("수신된 JSON 구조: {}", jsonNode.toString().substring(0, Math.min(200, jsonNode.toString().length())));

            // 기여모드 메시지 확인 (type 필드가 있는 경우)
            if (jsonNode.has("type") && "save_contribution_path".equals(jsonNode.get("type").asText())) {
                log.info("기여모드 메시지 감지됨 (type 필드): sessionId=[{}]", session.getId());
                handleContributionMessage(session, payload);
                return;
            }

            // 기여모드 메시지 확인 (legacy format: sessionId, task, steps 필드로 판단)
            if (jsonNode.has("sessionId") && jsonNode.has("task") && jsonNode.has("steps")
                && !jsonNode.has("toolName")) {
                log.info("기여모드 메시지 감지됨 (legacy format): sessionId=[{}]", session.getId());
                handleContributionMessage(session, payload);
                return;
            }

            // 기존 툴 실행 메시지 처리
            ControlDto.CallToolRequest request = objectMapper.readValue(payload, ControlDto.CallToolRequest.class);
            log.debug("도구 실행 요청 파싱 완료: toolName=[{}], argsCount=[{}]",
                    request.getToolName(), request.getArgs() != null ? request.getArgs().size() : 0);

            BrowserTool<?> tool = toolRegistry.getTool(request.getToolName());
            if (tool == null) {
                sendErrorResponse(session, ErrorMessages.Tool.TOOL_NOT_FOUND + request.getToolName());
                return;
            }

            if (!tool.isAvailable()) {
                sendErrorResponse(session, ErrorMessages.Tool.TOOL_NOT_AVAILABLE + request.getToolName());
                return;
            }

            ControlDto.ToolResult result = executeTool(tool, request.getArgs());

            String responseJson = objectMapper.writeValueAsString(result);
            session.sendMessage(new TextMessage(responseJson));

            log.info("도구 실행 완료 및 응답 전송: sessionId=[{}], toolName=[{}], success=[{}]",
                    session.getId(), request.getToolName(), !result.isError());

        } catch (JsonProcessingException e) {
            log.error("메시지 JSON 파싱 실패: sessionId=[{}], message=[{}]", session.getId(), payload, e);
            sendErrorResponse(session, ErrorMessages.WebSocket.INVALID_JSON_FORMAT + e.getMessage());
        } catch (Exception e) {
            log.error("메시지 처리 중 예상치 못한 오류 발생: sessionId=[{}]", session.getId(), e);
            sendErrorResponse(session, ErrorMessages.WebSocket.MESSAGE_PROCESSING_FAILED + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        controlService.unregisterSession(session);
        log.info("웹소켓 연결 종료: sessionId=[{}], status=[{}], reason=[{}]", 
                session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("웹소켓 전송 오류 발생: sessionId=[{}]", session.getId(), exception);
        super.handleTransportError(session, exception);
    }

    /**
     * 브라우저 툴을 실행하여 인자 값을 전달하고 결과를 반환
     *
     * @param tool 실행할 브라우저 툴
     * @param args 요청으로부터 전달된 원시 인자
     * @return 툴 실행 결과
     */
    @SuppressWarnings("unchecked")
    private ControlDto.ToolResult executeTool(BrowserTool<?> tool, Object args) {
        try {
            Object convertedArgs = objectMapper.convertValue(args, tool.getArgumentType());
            
            BrowserTool<Object> executableTool = (BrowserTool<Object>) tool;
            ControlDto.ToolResult result = executableTool.execute(convertedArgs);
            
            log.debug("도구 실행 성공: toolName=[{}], resultError=[{}]", 
                    tool.getName(), result.isError());
            
            return result;
            
        } catch (IllegalArgumentException e) {
            log.error("도구 인수 변환 실패: toolName=[{}], expectedType=[{}]", 
                    tool.getName(), tool.getArgumentType().getSimpleName(), e);
            
            return new ControlDto.ToolResult(
                java.util.List.of(new ControlDto.TextContent(
                    ErrorMessages.Tool.ARGUMENT_CONVERSION_FAILED + e.getMessage()
                )), 
                true
            );
        } catch (Exception e) {
            log.error("도구 실행 중 오류 발생: toolName=[{}]", tool.getName(), e);
            
            return new ControlDto.ToolResult(
                java.util.List.of(new ControlDto.TextContent(
                    ErrorMessages.Tool.TOOL_EXECUTION_FAILED + e.getMessage()
                )), 
                true
            );
        }
    }

    /**
     * 클라이언트에 오류 응답을 전송
     *
     * @param session WebSocket 세션
     * @param errorMessage 전송할 오류 메시지
     */
    private void sendErrorResponse(WebSocketSession session, String errorMessage) {
        try {
            ControlDto.ToolResult errorResult = new ControlDto.ToolResult(
                java.util.List.of(new ControlDto.TextContent(errorMessage)), 
                true
            );
            
            String errorJson = objectMapper.writeValueAsString(errorResult);
            session.sendMessage(new TextMessage(errorJson));
            
            log.debug("오류 응답 전송 완료: sessionId=[{}], error=[{}]", session.getId(), errorMessage);
            
        } catch (IOException e) {
            log.error("오류 응답 전송 실패: sessionId=[{}]", session.getId(), e);
        }
    }

    /**
     * 기여모드 메시지를 처리
     *
     * @param session WebSocket 세션
     * @param payload 기여모드 메시지 JSON
     */
    private void handleContributionMessage(WebSocketSession session, String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            ControlDto.ContributionMessage contributionMessage = new ControlDto.ContributionMessage();
            contributionMessage.setType("save_contribution_path");
            contributionMessage.setSessionId(jsonNode.get("sessionId").asText());
            contributionMessage.setTask(jsonNode.get("task").asText());

            List<ControlDto.ContributionStep> steps = objectMapper.convertValue(
                jsonNode.get("steps"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ControlDto.ContributionStep.class)
            );
            contributionMessage.setSteps(steps);

            if (jsonNode.has("isPartial")) {
                contributionMessage.setPartial(jsonNode.get("isPartial").asBoolean());
            }
            if (jsonNode.has("isComplete")) {
                contributionMessage.setComplete(jsonNode.get("isComplete").asBoolean());
            }
            if (jsonNode.has("totalSteps")) {
                contributionMessage.setTotalSteps(jsonNode.get("totalSteps").asInt());
            }

            log.info("기여모드 메시지 수신: sessionId=[{}], contributionSessionId=[{}], stepCount=[{}]",
                    session.getId(), contributionMessage.getSessionId(), contributionMessage.getSteps().size());

            mcpIntegrationService.sendContributionData(contributionMessage);

            ControlDto.ContributionResponse response = new ControlDto.ContributionResponse(
                    "contribution_response",
                    contributionMessage.getSessionId(),
                    true,
                    "기여모드 데이터가 성공적으로 저장되었습니다.",
                    contributionMessage.getSteps().size()
            );

            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));

            log.info("기여모드 처리 완료: sessionId=[{}], contributionSessionId=[{}]",
                    session.getId(), contributionMessage.getSessionId());

        } catch (Exception e) {
            log.error("기여모드 메시지 처리 실패: sessionId=[{}]", session.getId(), e);
            sendContributionErrorResponse(session, "기여모드 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 기여모드 오류 응답을 전송
     *
     * @param session WebSocket 세션
     * @param errorMessage 오류 메시지
     */
    private void sendContributionErrorResponse(WebSocketSession session, String errorMessage) {
        try {
            ControlDto.ContributionResponse errorResponse = new ControlDto.ContributionResponse(
                    "contribution_response",
                    "unknown",
                    false,
                    errorMessage,
                    0
            );

            String errorJson = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(errorJson));

            log.debug("기여모드 오류 응답 전송 완료: sessionId=[{}], error=[{}]", session.getId(), errorMessage);

        } catch (IOException e) {
            log.error("기여모드 오류 응답 전송 실패: sessionId=[{}]", session.getId(), e);
        }
    }

    /**
     * 새로 연결된 클라이언트에 환영 메시지를 전송
     *
     * @param session WebSocket 세션
     */
    private void sendWelcomeMessage(WebSocketSession session) {
        try {
            String welcomeMessage = String.format(
                ErrorMessages.WebSocket.WELCOME_MESSAGE_PREFIX + "%s",
                String.join(", ", toolRegistry.getAvailableToolNames())
            );

            ControlDto.ToolResult welcomeResult = new ControlDto.ToolResult(
                java.util.List.of(new ControlDto.TextContent(welcomeMessage)),
                false
            );

            String welcomeJson = objectMapper.writeValueAsString(welcomeResult);
            session.sendMessage(new TextMessage(welcomeJson));

            log.debug("환영 메시지 전송 완료: sessionId=[{}]", session.getId());

        } catch (IOException e) {
            log.warn("환영 메시지 전송 실패: sessionId=[{}]", session.getId(), e);
        }
    }
}