package com.vowser.backend.infrastructure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vowser.backend.application.service.ControlService;
import com.vowser.backend.api.dto.ControlDto;
import com.vowser.backend.common.constants.ErrorMessages;
import com.vowser.backend.common.constants.McpConstants;
import com.vowser.backend.common.constants.NetworkConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCP 서버와의 WebSocket 연결을 관리
 * 음성 명령을 처리하고 응답을 연결된 클라이언트로 중계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpWebSocketClient {

    private final ControlService controlService;
    private final ObjectMapper objectMapper;
    
    @Value("${mcp.server.url:ws://localhost:8000/ws}")
    private String mcpServerUrl;
    
    @Value("${mcp.reconnect.delay:20000}")
    private long reconnectDelayMs;
    

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(NetworkConstants.WebSocket.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(NetworkConstants.WebSocket.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    
    private volatile WebSocket webSocket;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @PostConstruct
    public void connect() {
        log.info("MCP 서버 연결 초기화 시작: url=[{}]", mcpServerUrl);
        establishConnection();
    }

    /**
     * MCP 서버와 WebSocket 연결을 수립
     */
    private void establishConnection() {
        if (isShuttingDown.get()) {
            log.info("애플리케이션 종료 중이므로 MCP 서버 연결을 시도하지 않습니다.");
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url(mcpServerUrl)
                    .build();

            webSocket = client.newWebSocket(request, new McpWebSocketListener());
            log.debug("MCP 서버 연결 요청 전송: url=[{}]", mcpServerUrl);
            
        } catch (Exception e) {
            log.error("MCP 서버 연결 요청 실패: url=[{}]", mcpServerUrl, e);
            scheduleReconnect();
        }
    }

    /**
     * 음성 명령을 MCP 서버로 전송
     *
     * @param transcript 음성 입력으로부터 변환된 텍스트
     * @param sessionId 클라이언트 세션 식별자
     */
    public void sendVoiceCommand(String transcript, String sessionId) {
        if (!isConnected.get()) {
            log.error("MCP 서버에 연결되어 있지 않습니다. 메시지 전송 실패: transcript=[{}], sessionId=[{}]", 
                    transcript, sessionId);
            return;
        }

        if (transcript == null || transcript.trim().isEmpty()) {
            log.warn("빈 음성 명령은 전송하지 않습니다: sessionId=[{}]", sessionId);
            return;
        }

        try {
            Map<String, Object> message = Map.of(
                "type", McpConstants.MessageTypes.SEARCH_PATH,
                "data", Map.of(
                    "query", transcript.trim(),
                    "limit", NetworkConstants.MCP.SEARCH_PATH_LIMIT,
                    "sessionId", sessionId
                )
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            boolean success = webSocket.send(jsonMessage);
            
            if (success) {
                log.info("MCP 서버로 음성 명령 전송 성공: sessionId=[{}], transcript=[{}]", 
                        sessionId, transcript);
            } else {
                log.error("MCP 서버로 메시지 전송 실패: WebSocket 전송 큐가 가득참");
            }
            
        } catch (Exception e) {
            log.error("음성 명령 JSON 직렬화 또는 전송 실패: sessionId=[{}], transcript=[{}]", 
                    sessionId, transcript, e);
        }
    }

    /**
     * 기여모드 데이터를 MCP 서버로 전송
     *
     * @param contributionMessage 기여모드 메시지
     */
    public void sendContributionData(ControlDto.ContributionMessage contributionMessage) {
        if (!isConnected.get()) {
            log.error("MCP 서버에 연결되어 있지 않습니다. 기여모드 데이터 전송 실패: sessionId=[{}]",
                    contributionMessage.getSessionId());
            return;
        }

        if (contributionMessage.getSteps() == null || contributionMessage.getSteps().isEmpty()) {
            log.warn("빈 기여모드 단계는 전송하지 않습니다: sessionId=[{}]", contributionMessage.getSessionId());
            return;
        }

        try {
            Map<String, Object> message = Map.of(
                "type", "save_contribution_path",
                "data", Map.of(
                    "sessionId", contributionMessage.getSessionId(),
                    "task", contributionMessage.getTask(),
                    "steps", contributionMessage.getSteps(),
                    "isPartial", contributionMessage.isPartial(),
                    "isComplete", contributionMessage.isComplete(),
                    "totalSteps", contributionMessage.getTotalSteps()
                )
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            boolean success = webSocket.send(jsonMessage);

            if (success) {
                log.info("MCP 서버로 기여모드 데이터 전송 성공: sessionId=[{}], stepCount=[{}]",
                        contributionMessage.getSessionId(), contributionMessage.getSteps().size());
            } else {
                log.error("MCP 서버로 기여모드 데이터 전송 실패: WebSocket 전송 큐가 가득참");
            }

        } catch (Exception e) {
            log.error("기여모드 데이터 JSON 직렬화 또는 전송 실패: sessionId=[{}]",
                    contributionMessage.getSessionId(), e);
        }
    }

    /**
     * MCP 서버와의 연결 여부를 확인
     *
     * @return 연결되어 있으면 true, 아니면 false
     */
    public boolean isConnected() {
        return isConnected.get() && webSocket != null;
    }

    /**
     * 지연 후 재연결을 스케줄링
     */
    private void scheduleReconnect() {
        if (isShuttingDown.get()) {
            return;
        }

        new Thread(() -> {
            try {
                log.info("MCP 서버 재연결 대기 중: {}ms 후 재시도", reconnectDelayMs);
                Thread.sleep(reconnectDelayMs);
                
                if (!isShuttingDown.get()) {
                    log.info("MCP 서버 재연결 시도...");
                    establishConnection();
                }
            } catch (InterruptedException e) {
                log.debug("재연결 대기 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
            }
        }, "mcp-reconnect-thread").start();
    }

    @PreDestroy
    public void disconnect() {
        log.info("MCP 클라이언트 종료 시작");
        isShuttingDown.set(true);
        
        if (webSocket != null) {
            webSocket.close(NetworkConstants.WebSocket.NORMAL_CLOSURE_CODE, ErrorMessages.MCP.APPLICATION_SHUTDOWN);
            log.info("MCP WebSocket 연결 종료 요청 완료");
        }
        
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        
        try {
            if (!client.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                client.dispatcher().executorService().shutdownNow();
            }
        } catch (InterruptedException e) {
            client.dispatcher().executorService().shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("MCP 클라이언트 종료 완료");
    }

    /**
     * MCP 서버 이벤트용 WebSocket 리스너
     */
    private class McpWebSocketListener extends WebSocketListener {
        
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            isConnected.set(true);
            log.info("MCP 서버 연결 성공: url=[{}], protocol=[{}]", 
                    mcpServerUrl, response.protocol());
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            log.info("MCP 서버에서 메시지 수신: messageLength=[{}]", text.length());
            log.debug("MCP 서버 메시지 내용: {}", text);
            
            try {
                controlService.relayMcpResponse(text);
                log.debug("MCP 응답 클라이언트 중계 완료");
            } catch (Exception e) {
                log.error("MCP 응답 중계 실패", e);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            isConnected.set(false);
            log.error("MCP 서버 연결 실패: response=[{}]", 
                    response != null ? response.code() + " " + response.message() : "null", t);
            
            if (!isShuttingDown.get()) {
                scheduleReconnect();
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            isConnected.set(false);
            log.warn("MCP 서버 연결 종료 중: code=[{}], reason=[{}]", code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            isConnected.set(false);
            log.warn("MCP 서버 연결 종료됨: code=[{}], reason=[{}]", code, reason);
            
            if (!isShuttingDown.get() && code != 1000) {
                scheduleReconnect();
            }
        }
    }
}