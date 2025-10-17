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
