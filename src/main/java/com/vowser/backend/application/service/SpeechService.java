package com.vowser.backend.application.service;

import com.google.cloud.speech.v2.*;
import com.google.protobuf.ByteString;
import com.vowser.backend.common.constants.ErrorMessages;
import com.vowser.backend.common.constants.NetworkConstants;
import com.vowser.backend.common.constants.SpeechConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Google Cloud Speech API를 사용해 음성을 텍스트로 변환
 * 업로드된 오디오 파일을 처리하고, 음성 명령용 변환 텍스트를 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechService {

    private final SpeechClient speechClient;

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.location}")
    private String location;

    /**
     * Google Cloud Speech-to-Text를 사용하여 오디오 파일을 텍스트로 변환
     *
     * @param audioFile 변환할 업로드된 오디오 파일
     * @return 오디오로부터 변환된 텍스트
     * @throws IllegalArgumentException 오디오 파일이 비어있거나 유효하지 않은 경우
     * @throws RuntimeException 변환(인식) 과정이 실패한 경우
     */
    public String transcribe(MultipartFile audioFile) {
        log.info("음성 파일 수신: {}, 크기: {} KB", 
                audioFile.getOriginalFilename(), 
                audioFile.getSize() / NetworkConstants.DataSize.BYTES_PER_KB);
        
        Assert.notNull(audioFile, "Audio file must not be null");
        Assert.isTrue(!audioFile.isEmpty(), ErrorMessages.Speech.EMPTY_AUDIO_FILE);

        try {
            ByteString audioData = ByteString.copyFrom(audioFile.getBytes());
            
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .addLanguageCodes(SpeechConstants.Language.KOREAN)
                    .setModel(SpeechConstants.Model.LONG)
                    .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
                    .build();

            String recognizerPath = String.format("projects/%s/locations/%s/recognizers/_",
                    projectId, location);

            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setRecognizer(recognizerPath)
                    .setConfig(config)
                    .setContent(audioData)
                    .build();

            log.info("Google Speech-to-Text API 호출 중... (recognizer: {})", recognizerPath);
            
            RecognizeResponse response = speechClient.recognize(request);

            if (response.getResultsList().isEmpty()) {
                throw new RuntimeException(ErrorMessages.Speech.NO_SPEECH_RECOGNIZED);
            }

            String transcript = response.getResults(0).getAlternatives(0).getTranscript();
            
            log.info("음성 인식 성공 - 변환된 텍스트: [{}]", transcript);
            return transcript.trim();

        } catch (IOException e) {
            log.error("음성 파일 읽기 실패: {}", audioFile.getOriginalFilename(), e);
            throw new RuntimeException(ErrorMessages.Speech.CANNOT_READ_AUDIO_FILE + e.getMessage(), e);
        } catch (Exception e) {
            log.error("음성 인식 처리 실패: {}", audioFile.getOriginalFilename(), e);
            throw new RuntimeException(ErrorMessages.Speech.SPEECH_RECOGNITION_FAILED + e.getMessage(), e);
        }
    }
}