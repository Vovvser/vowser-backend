package com.vowser.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vowser.backend.api.dto.NavigationDto;
import com.vowser.backend.common.constants.ApiConstants;
import com.vowser.backend.common.constants.McpConstants;
import com.vowser.backend.common.constants.ToolConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 백엔드, MCP 서버, 그리고 연결된 클라이언트 간의 통신을 관리하고
 * WebSocket 세션을 관리
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlService {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 새로운 WebSocket 세션을 등록
     *
     * @param session 등록할 WebSocket 세션
     */
    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("웹소켓 세션 등록: sessionId=[{}], totalSessions=[{}]", 
                session.getId(), sessions.size());
    }

    /**
     * WebSocket 세션을 등록 해제
     *
     * @param session 제거할 WebSocket 세션
     */
    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("웹소켓 세션 제거: sessionId=[{}], remainingSessions=[{}]", 
                session.getId(), sessions.size());
    }

    /**
     * WebSocket을 통해 연결된 클라이언트로 명령을 전송
     *
     * @param command 전송할 명령 데이터
     */
    public void sendCommandToClient(Map<String, Object> command) {
        WebSocketSession session = getLastConnectedSession();
        
        if (session == null || !session.isOpen()) {
            log.warn("명령 전송 실패: 연결된 클라이언트 세션이 없거나 닫혀있습니다. command=[{}]", command);
            return;
        }

        try {
            String jsonCommand = objectMapper.writeValueAsString(command);
            session.sendMessage(new TextMessage(jsonCommand));
            
            log.info("클라이언트로 명령 전송 완료: sessionId=[{}], command=[{}]", 
                    session.getId(), jsonCommand);
                    
        } catch (JsonProcessingException e) {
            log.error("명령 직렬화 실패: command=[{}]", command, e);
        } catch (IOException e) {
            log.error("명령 전송 중 IO 오류 발생: sessionId=[{}]", session.getId(), e);
        } catch (Exception e) {
            log.error("명령 전송 중 예상치 못한 오류 발생: sessionId=[{}]", session.getId(), e);
        }
    }

    /**
     * MCP 서버 응답을 연결된 클라이언트로 중계
     *
     * @param messageJson MCP 서버로부터 수신한 JSON 메시지
     */
    public void relayMcpResponse(String messageJson) {
        WebSocketSession session = getLastConnectedSession();
        
        if (session == null || !session.isOpen()) {
            log.warn("MCP 응답 중계 실패: 연결된 클라이언트 세션이 없거나 닫혀있습니다.");
            return;
        }

        try {
            NavigationDto.McpSearchPathResult mcpResponse = 
                    objectMapper.readValue(messageJson, NavigationDto.McpSearchPathResult.class);

            if (McpConstants.MessageTypes.SEARCH_PATH_RESULT.equals(mcpResponse.getType())) {
                NavigationDto.AllPathsResponse allPathsResponse = convertMcpToAllPathsResponse(mcpResponse);
                
                if (allPathsResponse != null && !allPathsResponse.getPaths().isEmpty()) {
                    Map<String, Object> command = Map.of(
                        "type", ApiConstants.BrowserCommands.ALL_NAVIGATION_PATHS,
                        "data", allPathsResponse
                    );
                    
                    String jsonCommand = objectMapper.writeValueAsString(command);
                    session.sendMessage(new TextMessage(jsonCommand));
                    
                    log.info("MCP 응답을 NavigationPath로 변환하여 클라이언트로 전송 완료: pathCount=[{}]", 
                            allPathsResponse.getPaths().size());
                } else {
                    log.info("변환할 유효한 경로가 없어 클라이언트로 전송하지 않음");
                }
            } else {
                session.sendMessage(new TextMessage(messageJson));
                log.info("MCP 서버 응답(type: {})을 클라이언트로 그대로 중계 완료", mcpResponse.getType());
            }
            
        } catch (JsonProcessingException e) {
            log.error("MCP 응답 JSON 파싱 실패: message=[{}]", messageJson, e);
        } catch (IOException e) {
            log.error("MCP 응답 중계 중 IO 오류 발생", e);
        } catch (Exception e) {
            log.error("MCP 응답 중계 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 가장 최근에 연결된 세션을 가져옴
     *
     * @return 마지막으로 연결된 WebSocket 세션 (없으면 null)
     */
    private WebSocketSession getLastConnectedSession() {
        return sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .reduce((first, second) -> second) // Get last element
                .orElse(null);
    }

    /**
     * MCP 응답을 AllPathsResponse DTO로 변환
     *
     * @param mcpResult MCP 서버 응답
     * @return 변환된 AllPathsResponse (실패 시 null)
     */
    private NavigationDto.AllPathsResponse convertMcpToAllPathsResponse(
            NavigationDto.McpSearchPathResult mcpResult) {
        
        if (!McpConstants.Status.SUCCESS.equals(mcpResult.getStatus()) || 
            mcpResult.getData().getMatchedPaths() == null || 
            mcpResult.getData().getMatchedPaths().isEmpty()) {
            
            log.info("MCP 응답 변환 실패: status=[{}], pathCount=[{}]", 
                    mcpResult.getStatus(), 
                    mcpResult.getData().getMatchedPaths() != null ? 
                            mcpResult.getData().getMatchedPaths().size() : 0);
            return null;
        }

        List<NavigationDto.McpMatchedPath> validPaths = mcpResult.getData().getMatchedPaths().stream()
                .filter(path -> path.getSteps().stream()
                        .noneMatch(step -> step.getUrl() != null && 
                                         step.getUrl().toLowerCase().contains(McpConstants.ExcludedDomains.EXAMPLE_COM)))
                .collect(Collectors.toList());

        if (validPaths.isEmpty()) {
            log.info("MCP 응답 변환 실패: 모든 경로가 example.com 테스트 경로입니다.");
            return null;
        }

        List<NavigationDto.PathDetail> pathDetails = validPaths.stream()
                .map(this::convertMcpPathToPathDetail)
                .collect(Collectors.toList());

        return new NavigationDto.AllPathsResponse(mcpResult.getData().getQuery(), pathDetails);
    }

    /**
     * 개별 MCP 경로를 PathDetail로 변환
     *
     * @param mcpPath MCP 매칭 경로
     * @return 변환된 PathDetail
     */
    private NavigationDto.PathDetail convertMcpPathToPathDetail(NavigationDto.McpMatchedPath mcpPath) {
        List<NavigationDto.NavigationStep> navigationSteps = mcpPath.getSteps().stream()
                .map(mcpStep -> {
                    String clientAction = determineClientAction(mcpStep);
                    Map<String, Object> htmlAttributes = null;
                    
                    if (ToolConstants.ClientActions.TYPE.equals(clientAction)) {
                        String inputValue = extractInputValueFromAction(mcpStep.getAction());
                        if (inputValue != null) {
                            htmlAttributes = Map.of("value", inputValue);
                        }
                    }
                    
                    return new NavigationDto.NavigationStep(
                            mcpStep.getUrl(),
                            mcpStep.getTitle(),
                            clientAction,
                            mcpStep.getSelector(),
                            htmlAttributes
                    );
                })
                .collect(Collectors.toList());

        return new NavigationDto.PathDetail(
                mcpPath.getPathId(),
                mcpPath.getScore(),
                mcpPath.getTotalWeight(),
                mcpPath.getLastUsed(),
                mcpPath.getEstimatedTime(),
                navigationSteps
        );
    }

    /**
     * MCP 단계 정보를 바탕으로 클라이언트 액션 타입을 결정
     *
     * @param mcpStep MCP 단계 정보
     * @return 클라이언트 액션 타입(navigate, click, type)
     */
    private String determineClientAction(NavigationDto.McpStep mcpStep) {
        String action = mcpStep.getAction().toLowerCase();
        
        if (action.contains(ToolConstants.ClientActions.NAVIGATE) || action.contains(ToolConstants.ClientActions.NAVIGATE_KR) || action.contains(ToolConstants.ClientActions.ACCESS_KR)) {
            return ToolConstants.ClientActions.NAVIGATE;
        } else if (action.contains(ToolConstants.ClientActions.CLICK) || action.contains(ToolConstants.ClientActions.CLICK_KR)) {
            return ToolConstants.ClientActions.CLICK;
        } else if (action.contains(ToolConstants.ClientActions.TYPE) || action.contains(ToolConstants.ClientActions.TYPE_KR)) {
            return ToolConstants.ClientActions.TYPE;
        }
        
        if (mcpStep.getSelector() == null || mcpStep.getSelector().trim().isEmpty()) {
            return ToolConstants.ClientActions.NAVIGATE;
        } else {
            return ToolConstants.ClientActions.CLICK;
        }
    }

    /**
     * 'type' 액션의 입력 값을 액션 문자열에서 추출합
     *
     * @param action 입력 값이 포함될 수 있는 액션 문자열
     * @return 추출된 입력 값(없으면 null)
     */
    private String extractInputValueFromAction(String action) {
        List<Pattern> patterns = List.of(
                Pattern.compile("입력.*?[:：]\\s*(.+)"),
                Pattern.compile("type.*?[:：]\\s*(.+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\"(.+)\"\\s*입력"),
                Pattern.compile("'(.+)'\\s*입력")
        );
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(action);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        
        return null;
    }

    /**
     * 모니터링용 현재 활성 세션 수를 반환
     *
     * @return 활성 세션 개수
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
}