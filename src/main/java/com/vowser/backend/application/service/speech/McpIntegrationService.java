package com.vowser.backend.application.service.speech;

import com.vowser.backend.api.dto.ControlDto;
import com.vowser.backend.infrastructure.mcp.McpWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpIntegrationService {

    private final McpWebSocketClient mcpWebSocketClient;

    public boolean isConnected() {
        return mcpWebSocketClient.isConnected();
    }

    public void sendVoiceCommand(String transcript, String sessionId) {
        if (!isConnected()) {
            log.error("MCP 서버에 연결되지 않음: sessionId=[{}]", sessionId);
            throw new IllegalStateException("MCP 서버에 연결되지 않음");
        }

        mcpWebSocketClient.sendVoiceCommand(transcript, sessionId);
        log.info("MCP 서버로 음성 명령 전송 완료: sessionId=[{}], transcript=[{}]", sessionId, transcript);
    }

    public void sendContributionData(ControlDto.ContributionMessage contributionMessage) {
        if (!isConnected()) {
            log.error("MCP 서버에 연결되지 않음: sessionId=[{}]", contributionMessage.getSessionId());
            throw new IllegalStateException("MCP 서버에 연결되지 않음");
        }

        mcpWebSocketClient.sendContributionData(contributionMessage);
        log.info("MCP 서버로 기여모드 데이터 전송 완료: sessionId=[{}], stepCount=[{}]",
                contributionMessage.getSessionId(), contributionMessage.getSteps().size());
    }
}