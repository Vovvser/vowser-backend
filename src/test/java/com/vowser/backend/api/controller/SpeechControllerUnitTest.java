package com.vowser.backend.api.controller;

import com.vowser.backend.api.dto.speech.SpeechResponse;
import com.vowser.backend.api.dto.speech.SpeechTranscribeRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeechControllerUnitTest {

    @Mock
    private SpeechProcessingService speechProcessingService;

    @InjectMocks
    private SpeechController speechController;

    private MockMultipartFile testAudioFile;
    private SpeechResponse successResponse;

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_TRANSCRIPT = "안녕하세요 테스트입니다";

    @BeforeEach
    void setUp() {
        testAudioFile = new MockMultipartFile(
                "audioFile", // DTO 필드 이름과 일치해야 함
                "test.wav",
                "audio/wav",
                "test audio content".getBytes()
        );

        successResponse = SpeechResponse.builder()
                .success(true)
                .transcript(TEST_TRANSCRIPT)
                .build();
    }

    private SpeechTranscribeRequest createTestRequest(List<String> customPhrases) {
        SpeechTranscribeRequest request = new SpeechTranscribeRequest();
        request.setAudioFile(testAudioFile);
        request.setSessionId(TEST_SESSION_ID);
        request.setCustomPhrases(customPhrases);
        return request;
    }

    @Test
    @DisplayName("음성 인식 요청 - 비로그인 사용자 성공")
    void transcribeAndExecute_NotLoggedIn_Success() {
        SpeechTranscribeRequest request = createTestRequest(null);
        given(speechProcessingService.processVoiceCommand(eq(request), any()))
                .willReturn(ResponseEntity.ok(successResponse));

        ResponseEntity<SpeechResponse> result = speechController.transcribeAndExecute(request, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(TEST_TRANSCRIPT, result.getBody().getTranscript());

        verify(speechProcessingService).processVoiceCommand(eq(request), eq(null));
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
}
