package com.vowser.backend.api.controller;

import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.application.service.speech.SpeechProcessingService;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SpeechControllerUnitTest {

    @Mock
    private SpeechProcessingService speechProcessingService;

    @InjectMocks
    private SpeechController speechController;

    private MockMultipartFile testAudioFile;
    private SpeechResponse successResponse;
    private SpeechResponse failureResponse;

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_TRANSCRIPT = "안녕하세요 테스트입니다";

    @BeforeEach
    void setUp() {
        testAudioFile = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                "test audio content".getBytes()
        );
        
        successResponse = SpeechResponse.builder()
                .success(true)
                .transcript(TEST_TRANSCRIPT)
                .build();

        failureResponse = SpeechResponse.builder()
                .success(false)
                .transcript(TEST_TRANSCRIPT)
                .message("MCP 서버에 연결되지 않음")
                .build();
    }

    @Test
    @DisplayName("음성 인식 요청 - 기본 파라미터로 성공")
    void transcribeAndExecute_BasicParameters_Success() {
        given(speechProcessingService.processVoiceCommand(any())).willReturn(ResponseEntity.ok(successResponse));

        ResponseEntity<SpeechResponse> result = speechController.transcribeAndExecute(
                testAudioFile, TEST_SESSION_ID, true, false, false, false, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());

        verify(speechProcessingService).processVoiceCommand(argThat(request -> 
            request.getSessionId().equals(TEST_SESSION_ID) &&
            request.isEnableGeneralMode() &&
            !request.isEnableNumberMode() &&
            !request.isEnableAlphabetMode() &&
            !request.isEnableSnippetMode() &&
            request.getCustomPhrases() == null
        ));
    }

    @Test
    @DisplayName("음성 인식 요청 - 모든 모드 활성화로 성공")
    void transcribeAndExecute_AllModesEnabled_Success() {
        List<String> customPhrases = List.of("커스텀1", "커스텀2");
        given(speechProcessingService.processVoiceCommand(any())).willReturn(ResponseEntity.ok(successResponse));

        ResponseEntity<SpeechResponse> result = speechController.transcribeAndExecute(
                testAudioFile, TEST_SESSION_ID, true, true, true, true, customPhrases);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());

        verify(speechProcessingService).processVoiceCommand(argThat(request -> 
            request.getSessionId().equals(TEST_SESSION_ID) &&
            request.isEnableGeneralMode() &&
            request.isEnableNumberMode() &&
            request.isEnableAlphabetMode() &&
            request.isEnableSnippetMode() &&
            request.getCustomPhrases() != null &&
            request.getCustomPhrases().size() == 2 &&
            request.getCustomPhrases().contains("커스텀1") &&
            request.getCustomPhrases().contains("커스텀2")
        ));
    }

    @Test
    @DisplayName("음성 인식 요청 - 서비스에서 실패 응답")
    void transcribeAndExecute_ServiceReturnsFailure_ReturnsError() {
        given(speechProcessingService.processVoiceCommand(any()))
                .willReturn(ResponseEntity.internalServerError().body(failureResponse));

        ResponseEntity<SpeechResponse> result = speechController.transcribeAndExecute(
                testAudioFile,
                TEST_SESSION_ID,
                true,
                false,
                false,
                false,
                null
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());
        assertEquals("MCP 서버에 연결되지 않음", result.getBody().getMessage());

        verify(speechProcessingService).processVoiceCommand(any());
    }

    @Test
    @DisplayName("MCP 연결 상태 확인 - 연결됨")
    void getMcpConnectionStatus_Connected_Success() {
        Map<String, Object> connectedResponse = Map.of(ApiConstants.RESPONSE_KEY_CONNECTED, true);
        given(speechProcessingService.getMcpConnectionStatus())
                .willReturn(ResponseEntity.ok(connectedResponse));

        ResponseEntity<Object> result = speechController.getMcpConnectionStatus();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals(true, responseBody.get(ApiConstants.RESPONSE_KEY_CONNECTED));

        verify(speechProcessingService).getMcpConnectionStatus();
    }

    @Test
    @DisplayName("MCP 연결 상태 확인 - 연결 안됨")
    void getMcpConnectionStatus_Disconnected_Success() {
        Map<String, Object> disconnectedResponse = Map.of(ApiConstants.RESPONSE_KEY_CONNECTED, false);
        given(speechProcessingService.getMcpConnectionStatus())
                .willReturn(ResponseEntity.ok(disconnectedResponse));

        ResponseEntity<Object> result = speechController.getMcpConnectionStatus();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals(false, responseBody.get(ApiConstants.RESPONSE_KEY_CONNECTED));

        verify(speechProcessingService).getMcpConnectionStatus();
    }

    @Test
    @DisplayName("SpeechTranscribeRequest 생성 확인")
    void transcribeAndExecute_CreatesCorrectRequest() {
        List<String> customPhrases = List.of("테스트");
        given(speechProcessingService.processVoiceCommand(any())).willReturn(ResponseEntity.ok(successResponse));

        speechController.transcribeAndExecute(
                testAudioFile,
                TEST_SESSION_ID,
                false,
                true,
                false,
                true,
                customPhrases
        );

        verify(speechProcessingService).processVoiceCommand(argThat(request -> {
            assertEquals(testAudioFile, request.getAudioFile());
            assertEquals(TEST_SESSION_ID, request.getSessionId());
            assertFalse(request.isEnableGeneralMode());
            assertTrue(request.isEnableNumberMode());
            assertFalse(request.isEnableAlphabetMode());
            assertTrue(request.isEnableSnippetMode());
            assertEquals(customPhrases, request.getCustomPhrases());
            return true;
        }));
    }

    @Test
    @DisplayName("빈 커스텀 phrase 리스트 처리")
    void transcribeAndExecute_EmptyCustomPhrases_HandlesCorrectly() {
        List<String> emptyPhrases = List.of();
        given(speechProcessingService.processVoiceCommand(any())).willReturn(ResponseEntity.ok(successResponse));

        speechController.transcribeAndExecute(
                testAudioFile,
                TEST_SESSION_ID,
                true,
                false,
                false,
                false,
                emptyPhrases
        );

        verify(speechProcessingService).processVoiceCommand(argThat(request ->
            request.getCustomPhrases() != null &&
            request.getCustomPhrases().isEmpty()
        ));
    }
}