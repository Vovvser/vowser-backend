package com.vowser.backend.application.service.speech;

import com.vowser.backend.common.constants.NetworkConstants;
import com.vowser.backend.common.enums.SpeechMode;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.common.exception.SpeechException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 네이버 클라우드 플랫폼 STT (Speech-to-Text)를 사용해 음성을 텍스트로 변환
 * 업로드된 오디오 파일을 처리하고, 음성 명령용 변환 텍스트를 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverSpeechService {

    private final SpeechModeService speechModeService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${naver.cloud.client-id}")
    private String clientId;

    @Value("${naver.cloud.client-secret}")
    private String clientSecret;

    @Value("${naver.cloud.stt.url:https://naveropenapi.apigw.ntruss.com/recog/v1/stt}")
    private String sttUrl;

    /**
     * 네이버 클라우드 STT를 사용하여 오디오 파일을 텍스트로 변환
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
            byte[] audioData = audioFile.getBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", clientSecret);
            headers.set("Content-Type", "application/octet-stream");

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(audioData, headers);

            String requestUrl = sttUrl + "?lang=Kor";

            log.info("네이버 클라우드 STT API 호출 중... (URL: {})", requestUrl);

            ResponseEntity<NaverSttResponse> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    requestEntity,
                    NaverSttResponse.class
            );

            if (response.getBody() == null || response.getBody().getText() == null) {
                throw new SpeechException(ErrorCode.SPEECH_NO_SPEECH_RECOGNIZED);
            }

            String transcript = response.getBody().getText();
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
     * @param customPhrases 추가 phrase hints (네이버 STT에서는 사용하지 않음)
     * @return 모드별 후처리가 적용된 인식 텍스트
     * @throws SpeechException 입력이 유효하지 않거나 처리 과정에서 오류가 발생한 경우
     */
    public String transcribeWithModes(MultipartFile audioFile, EnumSet<SpeechMode> modes, List<String> customPhrases) {
        log.info("모드 기반 음성 파일 수신: {}, 크기: {} KB, 활성화된 모드: {}",
                audioFile.getOriginalFilename(),
                audioFile.getSize() / NetworkConstants.DataSize.BYTES_PER_KB,
                modes);

        String rawTranscript = transcribe(audioFile);
        log.info("원본 음성 인식 결과: [{}]", rawTranscript);

        String processedTranscript = applyModePostProcessing(rawTranscript, modes);

        log.info("모드별 후처리 완료 - 최종 텍스트: [{}]", processedTranscript);
        return processedTranscript.trim();
    }

    private String applyModePostProcessing(String rawText, EnumSet<SpeechMode> modes) {
        String result = rawText;

        if (modes.contains(SpeechMode.NUMBER)) {
            result = normalizeNumbers(result);
        }

        return result;
    }

    private String normalizeNumbers(String text) {
        String result = text;

        result = convertComplexKoreanNumbers(result);

        result = result.replaceAll("\\b영\\b", "0")
                       .replaceAll("\\b공\\b", "0")
                       .replaceAll("\\b일\\b", "1")
                       .replaceAll("\\b이\\b", "2")
                       .replaceAll("\\b삼\\b", "3")
                       .replaceAll("\\b사\\b", "4")
                       .replaceAll("\\b오\\b", "5")
                       .replaceAll("\\b육\\b", "6")
                       .replaceAll("\\b칠\\b", "7")
                       .replaceAll("\\b팔\\b", "8")
                       .replaceAll("\\b구\\b", "9")
                       .replaceAll("\\b하나\\b", "1")
                       .replaceAll("\\b둘\\b", "2")
                       .replaceAll("\\b셋\\b", "3")
                       .replaceAll("\\b넷\\b", "4")
                       .replaceAll("\\b다섯\\b", "5")
                       .replaceAll("\\b여섯\\b", "6")
                       .replaceAll("\\b일곱\\b", "7")
                       .replaceAll("\\b여덟\\b", "8")
                       .replaceAll("\\b아홉\\b", "9")
                       .replaceAll("\\b열\\b", "10");

        return result;
    }

    private String convertComplexKoreanNumbers(String text) {
        String result = text;

        Pattern hundredsPattern = Pattern.compile("([일이삼사오육칠팔구]?)백([일이삼사오육칠팔구]?)십([일이삼사오육칠팔구]?)");
        Matcher hundredsMatcher = hundredsPattern.matcher(result);
        StringBuffer sb1 = new StringBuffer();
        while (hundredsMatcher.find()) {
            String hundreds = hundredsMatcher.group(1);
            String tens = hundredsMatcher.group(2);
            String ones = hundredsMatcher.group(3);

            int h = hundreds.isEmpty() ? 1 : convertSingleKoreanDigit(hundreds);
            int t = tens.isEmpty() ? 0 : convertSingleKoreanDigit(tens);
            int o = ones.isEmpty() ? 0 : convertSingleKoreanDigit(ones);

            hundredsMatcher.appendReplacement(sb1, String.valueOf(h * 100 + t * 10 + o));
        }
        hundredsMatcher.appendTail(sb1);
        result = sb1.toString();

        Pattern tensPattern = Pattern.compile("([일이삼사오육칠팔구]?)십([일이삼사오육칠팔구]?)");
        Matcher tensMatcher = tensPattern.matcher(result);
        StringBuffer sb2 = new StringBuffer();
        while (tensMatcher.find()) {
            String tens = tensMatcher.group(1);
            String ones = tensMatcher.group(2);

            int t = tens.isEmpty() ? 1 : convertSingleKoreanDigit(tens);
            int o = ones.isEmpty() ? 0 : convertSingleKoreanDigit(ones);

            tensMatcher.appendReplacement(sb2, String.valueOf(t * 10 + o));
        }
        tensMatcher.appendTail(sb2);

        return sb2.toString();
    }

    private int convertSingleKoreanDigit(String digit) {
        switch (digit) {
            case "일": return 1;
            case "이": return 2;
            case "삼": return 3;
            case "사": return 4;
            case "오": return 5;
            case "육": return 6;
            case "칠": return 7;
            case "팔": return 8;
            case "구": return 9;
            default: return 0;
        }
    }

    /**
     * 네이버 STT API 응답 DTO
     */
    public static class NaverSttResponse {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}