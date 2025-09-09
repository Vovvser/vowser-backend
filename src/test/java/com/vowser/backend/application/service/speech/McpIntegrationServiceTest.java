package com.vowser.backend.application.service.speech;

import com.vowser.backend.infrastructure.mcp.McpWebSocketClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class McpIntegrationServiceTest {

    @Mock
    private McpWebSocketClient mcpWebSocketClient;

    @InjectMocks
    private McpIntegrationService mcpIntegrationService;

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_TRANSCRIPT = "안녕하세요 테스트입니다";

    @Test
    @DisplayName("MCP 연결 상태가 true인 경우")
    void isConnected_WhenMcpConnected_ReturnsTrue() {
        given(mcpWebSocketClient.isConnected()).willReturn(true);

        boolean result = mcpIntegrationService.isConnected();

        assertTrue(result);
        verify(mcpWebSocketClient).isConnected();
    }

    @Test
    @DisplayName("MCP 연결 상태가 false인 경우")
    void isConnected_WhenMcpDisconnected_ReturnsFalse() {
        given(mcpWebSocketClient.isConnected()).willReturn(false);

        boolean result = mcpIntegrationService.isConnected();

        assertFalse(result);
        verify(mcpWebSocketClient).isConnected();
    }

    @Test
    @DisplayName("연결된 상태에서 음성 명령 전송 성공")
    void sendVoiceCommand_WhenConnected_SendsSuccessfully() {
        given(mcpWebSocketClient.isConnected()).willReturn(true);
        willDoNothing().given(mcpWebSocketClient).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        assertDoesNotThrow(() -> {
            mcpIntegrationService.sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
        });

        verify(mcpWebSocketClient).isConnected();
        verify(mcpWebSocketClient).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("연결되지 않은 상태에서 음성 명령 전송 시 예외 발생")
    void sendVoiceCommand_WhenNotConnected_ThrowsIllegalStateException() {
        given(mcpWebSocketClient.isConnected()).willReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            mcpIntegrationService.sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
        });

        assertEquals("MCP 서버에 연결되지 않음", exception.getMessage());
        verify(mcpWebSocketClient).isConnected();
        verify(mcpWebSocketClient, never()).sendVoiceCommand(any(), any());
    }

    @Test
    @DisplayName("MCP 클라이언트에서 예외 발생 시 그대로 전파")
    void sendVoiceCommand_WhenMcpClientThrowsException_PropagatesException() {
        given(mcpWebSocketClient.isConnected()).willReturn(true);
        willThrow(new RuntimeException("MCP 전송 오류")).given(mcpWebSocketClient)
                .sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mcpIntegrationService.sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
        });

        assertEquals("MCP 전송 오류", exception.getMessage());
        verify(mcpWebSocketClient).isConnected();
        verify(mcpWebSocketClient).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("빈 transcript로 전송 시도")
    void sendVoiceCommand_WithEmptyTranscript_StillCallsMcpClient() {
        String emptyTranscript = "";
        given(mcpWebSocketClient.isConnected()).willReturn(true);
        willDoNothing().given(mcpWebSocketClient).sendVoiceCommand(emptyTranscript, TEST_SESSION_ID);

        assertDoesNotThrow(() -> {
            mcpIntegrationService.sendVoiceCommand(emptyTranscript, TEST_SESSION_ID);
        });

        verify(mcpWebSocketClient).isConnected();
        verify(mcpWebSocketClient).sendVoiceCommand(emptyTranscript, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("null transcript로 전송 시도")
    void sendVoiceCommand_WithNullTranscript_StillCallsMcpClient() {
        given(mcpWebSocketClient.isConnected()).willReturn(true);
        willDoNothing().given(mcpWebSocketClient).sendVoiceCommand(null, TEST_SESSION_ID);

        assertDoesNotThrow(() -> {
            mcpIntegrationService.sendVoiceCommand(null, TEST_SESSION_ID);
        });

        verify(mcpWebSocketClient).isConnected();
        verify(mcpWebSocketClient).sendVoiceCommand(null, TEST_SESSION_ID);
    }
}