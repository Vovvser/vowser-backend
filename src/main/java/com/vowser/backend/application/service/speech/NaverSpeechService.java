package com.vowser.backend.application.service.speech;

import com.vowser.backend.application.service.speech.normalizer.AlphabetNormalizer;
import com.vowser.backend.application.service.speech.normalizer.NumberNormalizer;
import com.vowser.backend.common.constants.NetworkConstants;
import com.vowser.backend.common.enums.SpeechMode;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.common.exception.SpeechException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

/**
 * 네이버 클라우드 플랫폼 STT (Speech-to-Text)를 사용해 음성을 텍스트로 변환
 * 업로드된 오디오 파일을 처리하고, 음성 명령용 변환 텍스트를 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverSpeechService {

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(10_000);
        return new RestTemplate(f);
    }

    @Value("${naver.cloud.client-id}")
    private String clientId;

    @Value("${naver.cloud.client-secret}")
    private String clientSecret;

    @Value("${naver.cloud.stt.url:https://naveropenapi.apigw.ntruss.com/recog/v1/stt}")
    private String sttUrl;

    @Value("${naver.cloud.stt.lang:Kor}")
    private String defaultLang;

    /**
     * 네이버 클라우드 STT를 사용하여 오디오 파일을 텍스트로 변환
     *
     * @param audioFile 변환할 오디오 파일
     * @return 인식된 텍스트
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
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(audioData, headers);

            String requestUrl = sttUrl + "?lang=" + defaultLang;

            log.info("네이버 STT API 호출 (URL: {})", requestUrl);

            ResponseEntity<NaverSttResponse> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    requestEntity,
                    NaverSttResponse.class
            );

            if (response.getBody() == null || response.getBody().getText() == null) {
                throw new SpeechException(ErrorCode.SPEECH_NO_SPEECH_RECOGNIZED);
            }

            String transcript = response.getBody().getText().trim();

            log.debug("음성 인식 성공 - (마스킹) [{}]", maskPII(transcript));
            return transcript;

        } catch (IOException e) {
            log.error("음성 파일 읽기 실패: {}", audioFile.getOriginalFilename(), e);
            throw new SpeechException(ErrorCode.SPEECH_CANNOT_READ_AUDIO_FILE);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("STT API 응답 에러: status={}, body={}", e.getRawStatusCode(), body);
            throw new SpeechException(ErrorCode.SPEECH_RECOGNITION_FAILED, "stt http " + e.getRawStatusCode());
        } catch (Exception e) {
            log.error("음성 인식 처리 실패: {}", audioFile.getOriginalFilename(), e);
            throw new SpeechException(ErrorCode.SPEECH_RECOGNITION_FAILED);
        }
    }

    /**
     * 모드 기반 음성 인식 처리
     */
    public String transcribeWithModes(MultipartFile audioFile, EnumSet<SpeechMode> modes, List<String> customPhrases) {
        log.info("모드 기반 음성 파일 수신: {}, 크기: {} KB, 활성화된 모드: {}",
                audioFile.getOriginalFilename(),
                audioFile.getSize() / NetworkConstants.DataSize.BYTES_PER_KB,
                modes);

        String rawTranscript = transcribe(audioFile);
        log.debug("원본 인식 결과(마스킹): [{}]", maskPII(rawTranscript));

        String processedTranscript = applyModePostProcessing(rawTranscript, modes);

        log.info("모드별 후처리 완료");
        log.debug("최종 텍스트(마스킹): [{}]", maskPII(processedTranscript));
        return processedTranscript.trim();
    }

    private String applyModePostProcessing(String rawText, EnumSet<SpeechMode> modes) {
        String result = rawText;
        if (modes != null) {
            if (modes.contains(SpeechMode.NUMBER)) {
                result = NumberNormalizer.normalize(result);
            }
            if (modes.contains(SpeechMode.ALPHABET)) {
                result = AlphabetNormalizer.normalize(result);
            }
        }
        return result;
    }

    private static String maskPII(String s) {
        if (s == null) return null;
        String masked = s;
        masked = masked.replaceAll("(01[016789]-?\\d{3,4}-?\\d{4})", "XXX-XXXX-XXXX");
        masked = masked.replaceAll("(\\d{6})-?(\\d{7})", "XXXXXX-XXXXXXX");
        return masked;
    }

    /**
     * 네이버 STT API 응답 DTO
     */
    public static class NaverSttResponse {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}