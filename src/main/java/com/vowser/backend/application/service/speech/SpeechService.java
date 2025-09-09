package com.vowser.backend.application.service.speech;

import com.google.cloud.speech.v2.*;
import com.google.protobuf.ByteString;
import com.vowser.backend.common.constants.NetworkConstants;
import com.vowser.backend.common.constants.SpeechConstants;
import com.vowser.backend.common.enums.SpeechMode;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.common.exception.SpeechException;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private final SpeechModeService speechModeService;

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.location}")
    private String location;

    /**
     * Google Cloud Speech-to-Text를 사용하여 오디오 파일을 텍스트로 변환
     *
     * @param audioFile 변환할 오디오 파일
     * @return 인식된 텍스트(양끝 공백 제거)
     * @throws SpeechException 입력이 유효하지 않거나 파일 읽기/인식 과정에서 오류가 발생한 경우
     */
    public String transcribe(MultipartFile audioFile) {

        if (audioFile == null) {
            throw new SpeechException(ErrorCode.INVALID_INPUT_VALUE, "audioFile is null");
        }
        if (audioFile.isEmpty()) {
            throw new SpeechException(
                    ErrorCode.SPEECH_EMPTY_AUDIO_FILE,
                    "filename=" + audioFile.getOriginalFilename()
            );
        }
        log.info("음성 파일 수신: {}, 크기: {} KB",
                audioFile.getOriginalFilename(),
                audioFile.getSize() / NetworkConstants.DataSize.BYTES_PER_KB);


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
                throw new SpeechException(ErrorCode.SPEECH_NO_SPEECH_RECOGNIZED);
            }

            String transcript = response.getResults(0).getAlternatives(0).getTranscript();

            log.info("음성 인식 성공 - 변환된 텍스트: [{}]", transcript);
            return transcript.trim();

        } catch (IOException e) {
            log.error("음성 파일 읽기 실패: {}", audioFile.getOriginalFilename(), e);
            throw new SpeechException(ErrorCode.SPEECH_CANNOT_READ_AUDIO_FILE);
        } catch (Exception e) {
            log.error("음성 인식 처리 실패: {}", audioFile.getOriginalFilename(), e);
            throw new SpeechException(ErrorCode.SPEECH_RECOGNITION_FAILED);
        }
    }
    
    /**
     * 모드 기반 음성 인식 처리
     * 
     * @param audioFile 변환할 오디오 파일
     * @param modes 활성화된 인식 모드들
     * @param customPhrases 추가 phrase hints
     * @return 모드별 후처리가 적용된 인식 텍스트
     * @throws SpeechException 입력이 유효하지 않거나 처리 과정에서 오류가 발생한 경우
     */
    public String transcribeWithModes(MultipartFile audioFile, EnumSet<SpeechMode> modes, List<String> customPhrases) {
        if (audioFile == null) {
            throw new SpeechException(ErrorCode.INVALID_INPUT_VALUE, "audioFile is null");
        }
        if (audioFile.isEmpty()) {
            throw new SpeechException(
                    ErrorCode.SPEECH_EMPTY_AUDIO_FILE,
                    "filename=" + audioFile.getOriginalFilename()
            );
        }
        
        log.info("모드 기반 음성 파일 수신: {}, 크기: {} KB, 활성화된 모드: {}",
                audioFile.getOriginalFilename(),
                audioFile.getSize() / NetworkConstants.DataSize.BYTES_PER_KB,
                modes);

        try {
            ByteString audioData = ByteString.copyFrom(audioFile.getBytes());

            RecognitionConfig config = buildConfigForModes(modes, customPhrases);

            String recognizerPath = String.format("projects/%s/locations/%s/recognizers/_",
                    projectId, location);

            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setRecognizer(recognizerPath)
                    .setConfig(config)
                    .setContent(audioData)
                    .build();

            log.info("모드별 Google Speech-to-Text API 호출 중... (recognizer: {})", recognizerPath);

            RecognizeResponse response = speechClient.recognize(request);

            if (response.getResultsList().isEmpty()) {
                throw new SpeechException(ErrorCode.SPEECH_NO_SPEECH_RECOGNIZED);
            }

            String rawTranscript = response.getResults(0).getAlternatives(0).getTranscript();
            log.info("원본 음성 인식 결과: [{}]", rawTranscript);

            String processedTranscript = applyModePostProcessing(rawTranscript, modes);
            
            log.info("모드별 후처리 완료 - 최종 텍스트: [{}]", processedTranscript);
            return processedTranscript.trim();

        } catch (IOException e) {
            log.error("음성 파일 읽기 실패: {}", audioFile.getOriginalFilename(), e);
            throw new SpeechException(ErrorCode.SPEECH_CANNOT_READ_AUDIO_FILE);
        } catch (Exception e) {
            log.error("모드별 음성 인식 처리 실패: {}", audioFile.getOriginalFilename(), e);
            throw new SpeechException(ErrorCode.SPEECH_RECOGNITION_FAILED);
        }
    }

    private RecognitionConfig buildConfigForModes(EnumSet<SpeechMode> modes, List<String> customPhrases) {
        List<String> phraseHints = speechModeService.buildPhraseHints(modes, customPhrases);
        
        RecognitionConfig.Builder configBuilder = RecognitionConfig.newBuilder()
                .addLanguageCodes(SpeechConstants.Language.KOREAN)
                .setModel(SpeechConstants.Model.LONG)
                .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build());
        
        log.info("Phrase hints 준비됨: {} 개의 힌트 (Google Cloud Speech v2에서 런타임 적용)", phraseHints.size());
        
        return configBuilder.build();
    }

    private String applyModePostProcessing(String rawText, EnumSet<SpeechMode> modes) {
        String result = rawText;
        
        if (modes.contains(SpeechMode.NUMBER)) {
            result = normalizeNumbers(result);
        }
        
        return result;
    }

    private String normalizeNumbers(String text) {
        return text.replaceAll("영", "0")
                   .replaceAll("공", "0")
                   .replaceAll("일", "1")
                   .replaceAll("이", "2")
                   .replaceAll("삼", "3")
                   .replaceAll("사", "4")
                   .replaceAll("오", "5")
                   .replaceAll("육", "6")
                   .replaceAll("칠", "7")
                   .replaceAll("팔", "8")
                   .replaceAll("구", "9")
                   .replaceAll("하나", "1")
                   .replaceAll("둘", "2")
                   .replaceAll("셋", "3")
                   .replaceAll("넷", "4")
                   .replaceAll("다섯", "5")
                   .replaceAll("여섯", "6")
                   .replaceAll("일곱", "7")
                   .replaceAll("여덟", "8")
                   .replaceAll("아홉", "9")
                   .replaceAll("열", "10");
    }

}