package com.vowser.backend.application.service.speech;

import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
import com.vowser.backend.common.constants.ApiConstants;
import com.vowser.backend.common.enums.SpeechMode;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SpeechProcessingServiceTest {

    @Mock
    private SpeechService speechService;

    @Mock
    private SpeechModeService speechModeService;

    @Mock
    private McpIntegrationService mcpIntegrationService;

    @InjectMocks
    private SpeechProcessingService speechProcessingService;

    private MockMultipartFile testAudioFile;
    private SpeechTranscribeRequest basicRequest;
    private SpeechTranscribeRequest specialModeRequest;

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_TRANSCRIPT = "안녕하세요 테스트입니다";

    @BeforeEach
    void setUp() {
        testAudioFile = new MockMultipartFile("audio", "test.wav", "audio/wav", "test audio content".getBytes());
        
        basicRequest = new SpeechTranscribeRequest(
            testAudioFile, TEST_SESSION_ID, true, false, false, false, null);
        
        specialModeRequest = new SpeechTranscribeRequest(
            testAudioFile, TEST_SESSION_ID, true, true, false, false, List.of("커스텀"));
    }

    @Test
    @DisplayName("기본 모드 처리 - 특수 모드 미사용 시 기본 transcribe 호출")
    void processVoiceCommand_BasicMode_UseBasicTranscribe() {
        given(speechService.transcribe(testAudioFile)).willReturn(TEST_TRANSCRIPT);
        willDoNothing().given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(basicRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());

        verify(speechService).transcribe(testAudioFile);
        verify(speechService, never()).transcribeWithModes(any(), any(), any());
        verify(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("특수 모드 처리 - 숫자 모드 활성화 시 모드별 transcribe 호출")
    void processVoiceCommand_SpecialMode_UseModesTranscribe() {
        EnumSet<SpeechMode> expectedModes = EnumSet.of(SpeechMode.GENERAL, SpeechMode.NUMBER);
        given(speechModeService.buildModes(true, true, false, false)).willReturn(expectedModes);
        given(speechService.transcribeWithModes(testAudioFile, expectedModes, List.of("커스텀")))
                .willReturn(TEST_TRANSCRIPT);
        willDoNothing().given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(specialModeRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());

        verify(speechService, never()).transcribe(any());
        verify(speechService).transcribeWithModes(testAudioFile, expectedModes, List.of("커스텀"));
        verify(speechModeService).buildModes(true, true, false, false);
        verify(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("커스텀 phrase만 있어도 모드별 처리 사용")
    void processVoiceCommand_WithCustomPhrases_UseModesTranscribe() {
        SpeechTranscribeRequest customPhraseRequest = new SpeechTranscribeRequest(
            testAudioFile, TEST_SESSION_ID, true, false, false, false, List.of("테스트"));
        
        EnumSet<SpeechMode> expectedModes = EnumSet.of(SpeechMode.GENERAL);
        given(speechModeService.buildModes(true, false, false, false)).willReturn(expectedModes);
        given(speechService.transcribeWithModes(testAudioFile, expectedModes, List.of("테스트")))
                .willReturn(TEST_TRANSCRIPT);
        willDoNothing().given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(customPhraseRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(speechService).transcribeWithModes(testAudioFile, expectedModes, List.of("테스트"));
        verify(speechService, never()).transcribe(any());
    }

    @Test
    @DisplayName("MCP 연결 실패 시 에러 응답")
    void processVoiceCommand_McpConnectionFailed_ReturnsErrorResponse() {
        given(speechService.transcribe(testAudioFile)).willReturn(TEST_TRANSCRIPT);
        willThrow(new IllegalStateException("MCP 서버에 연결되지 않음"))
                .given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(basicRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());
        assertEquals("MCP 서버에 연결되지 않음", result.getBody().getMessage());

        verify(speechService).transcribe(testAudioFile);
        verify(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);
    }

    @Test
    @DisplayName("MCP 연결 상태 확인 - 연결됨")
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

    @Test
    @DisplayName("MCP 연결 상태 확인 - 연결 안됨")
    void getMcpConnectionStatus_Disconnected_ReturnsFalse() {
        given(mcpIntegrationService.isConnected()).willReturn(false);

        ResponseEntity<Object> result = speechProcessingService.getMcpConnectionStatus();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals(false, responseBody.get(ApiConstants.RESPONSE_KEY_CONNECTED));
        
        verify(mcpIntegrationService).isConnected();
    }

    @Test
    @DisplayName("빈 커스텀 phrase 리스트는 기본 모드 사용")
    void processVoiceCommand_EmptyCustomPhrases_UseBasicTranscribe() {
        SpeechTranscribeRequest emptyCustomRequest = new SpeechTranscribeRequest(
            testAudioFile, TEST_SESSION_ID, true, false, false, false, List.of());
        
        given(speechService.transcribe(testAudioFile)).willReturn(TEST_TRANSCRIPT);
        willDoNothing().given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(emptyCustomRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(speechService).transcribe(testAudioFile);
        verify(speechService, never()).transcribeWithModes(any(), any(), any());
    }

    @Test
    @DisplayName("모든 특수 모드 활성화된 경우")
    void processVoiceCommand_AllSpecialModesEnabled_UseModesTranscribe() {
        SpeechTranscribeRequest allModesRequest = new SpeechTranscribeRequest(
            testAudioFile, TEST_SESSION_ID, true, true, true, true, null);
        
        EnumSet<SpeechMode> expectedModes = EnumSet.of(
            SpeechMode.GENERAL, SpeechMode.NUMBER, SpeechMode.ALPHABET, SpeechMode.SNIPPET);
        given(speechModeService.buildModes(true, true, true, true)).willReturn(expectedModes);
        given(speechService.transcribeWithModes(testAudioFile, expectedModes, null))
                .willReturn(TEST_TRANSCRIPT);
        willDoNothing().given(mcpIntegrationService).sendVoiceCommand(TEST_TRANSCRIPT, TEST_SESSION_ID);

        ResponseEntity<SpeechResponse> result = speechProcessingService.processVoiceCommand(allModesRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(speechService).transcribeWithModes(testAudioFile, expectedModes, null);
        verify(speechModeService).buildModes(true, true, true, true);
    }
}