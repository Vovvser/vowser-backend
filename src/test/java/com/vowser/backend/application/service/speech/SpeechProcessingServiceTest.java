package com.vowser.backend.application.service.speech;

import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
import com.vowser.backend.application.service.AccessibilityProfileService;
import com.vowser.backend.common.constants.ApiConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SpeechProcessingServiceTest {

    @Mock
    private SpeechService speechService;

    @Mock
    private McpIntegrationService mcpIntegrationService;

    @Mock
    private AccessibilityProfileService accessibilityProfileService;

    @InjectMocks
    private SpeechProcessingService speechProcessingService;

    private MockMultipartFile testAudioFile;
    private SpeechTranscribeRequest basicRequest;

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_TRANSCRIPT = "안녕하세요 테스트입니다";

    @BeforeEach
    void setUp() {
        testAudioFile = new MockMultipartFile("audio", "test.wav", "audio/wav", "test audio content".getBytes());
        basicRequest = new SpeechTranscribeRequest();
        basicRequest.setAudioFile(testAudioFile);
        basicRequest.setSessionId(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("기본 모드 처리 - 비로그인, 임시 프로필 없음")
    void processVoiceCommand_BasicMode_NoProfile() {
        given(speechService.transcribe(testAudioFile)).willReturn(TEST_TRANSCRIPT);
        willDoNothing().given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(basicRequest, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());

        verify(accessibilityProfileService, never()).findProfileByMemberId(any());
        verify(speechService).transcribe(testAudioFile);
        verify(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("MCP 연결 실패 시 에러 응답")
    void processVoiceCommand_McpConnectionFailed_ReturnsErrorResponse() {
        given(speechService.transcribe(testAudioFile)).willReturn(TEST_TRANSCRIPT);
        willThrow(new IllegalStateException("MCP 서버에 연결되지 않음"))
                .given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(basicRequest, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals("MCP 서버에 연결되지 않음", result.getBody().getMessage());
    }

    @Test
    @DisplayName("MCP 연결 상태 확인")
    void getMcpConnectionStatus_Connected_ReturnsTrue() {
        given(mcpIntegrationService.isConnected()).willReturn(true);

        ResponseEntity<Object> result = speechProcessingService.getMcpConnectionStatus();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals(true, responseBody.get(ApiConstants.RESPONSE_KEY_CONNECTED));

        verify(mcpIntegrationService).isConnected();
    }
}
