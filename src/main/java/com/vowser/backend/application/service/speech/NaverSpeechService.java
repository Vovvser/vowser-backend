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
import java.util.*;
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
            result = NumberNormalizer.normalize(result);
        }
        if (modes.contains(SpeechMode.ALPHABET)) {
            result = AlphabetNormalizer.normalize(result);
        }

        return result;
    }

    /**
     * 숫자 정규화 유틸
     */
    private static final class NumberNormalizer {

        private static final Map<String, String> BASIC = new LinkedHashMap<>();
        static {
            BASIC.put("영", "0"); BASIC.put("공", "0"); BASIC.put("제로", "0");
            BASIC.put("하나", "1"); BASIC.put("일", "1");
            BASIC.put("둘", "2"); BASIC.put("이", "2");
            BASIC.put("셋", "3"); BASIC.put("삼", "3");
            BASIC.put("넷", "4"); BASIC.put("사", "4");
            BASIC.put("다섯", "5"); BASIC.put("오", "5");
            BASIC.put("여섯", "6"); BASIC.put("육", "6");
            BASIC.put("일곱", "7"); BASIC.put("칠", "7");
            BASIC.put("여덟", "8"); BASIC.put("팔", "8");
            BASIC.put("아홉", "9"); BASIC.put("구", "9");
        }

        private static final Pattern WORD_BOUNDARY_TOKEN = Pattern.compile("\\b(영|공|제로|하나|일|둘|이|셋|삼|넷|사|다섯|오|여섯|육|일곱|칠|여덟|팔|아홉|구)\\b");
        private static final Pattern DECIMAL_POINT = Pattern.compile("(\\d)\\s*점\\s*(\\d+)");
        private static final List<String> PHONE_HINTS = Arrays.asList("전화", "번호", "연락처", "휴대폰", "핸드폰");

        private static final Map<Character, Integer> UNIT = Map.of('십',10,'백',100,'천',1000);
        private static final Map<Character, Integer> DIGIT = Map.ofEntries(
                Map.entry('영', 0),
                Map.entry('공', 0),
                Map.entry('일', 1),
                Map.entry('이', 2),
                Map.entry('삼', 3),
                Map.entry('사', 4),
                Map.entry('오', 5),
                Map.entry('육', 6),
                Map.entry('칠', 7),
                Map.entry('팔', 8),
                Map.entry('구', 9)
        );

        static String normalize(String input) {
            if (input == null || input.isBlank()) return input;

            String s = input;

            // 1) 소수점(점) 처리: 숫자 점 숫자 → 숫자.숫자
            s = DECIMAL_POINT.matcher(s).replaceAll("$1.$2");

            // 2) 단어 단위 숫자 치환(동의어 포함)
            s = replaceBasicDigitsByWordBoundary(s);

            // 3) 억/만/천/백/십 한자어 수사 → 정수로 치환 (문자열 전체에서 발견되는 패턴 대상)
            s = replaceSinoKoreanNumbers(s);

            // 4) 전화번호/번호 문맥에서 한 자릿수 나열 결합 및 하이픈 포맷
            s = normalizePhoneLikeSequencesWhenContext(s);

            // 5) 숫자 사이 과도한 공백 정리
            s = s.replaceAll("(?<=\\d)\\s+(?=\\d)", ""); // 1   2  → 12
            s = s.replaceAll("\\s+", " ").trim();
            return s;
        }

        private static String replaceBasicDigitsByWordBoundary(String s) {
            Matcher m = WORD_BOUNDARY_TOKEN.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String key = m.group(1);
                m.appendReplacement(sb, BASIC.getOrDefault(key, key));
            }
            m.appendTail(sb);
            return sb.toString();
        }

        private static String replaceSinoKoreanNumbers(String s) {
            Pattern block = Pattern.compile("(?<![가-힣])(영|공|일|이|삼|사|오|육|칠|팔|구|십|백|천|만|억){2,}(?![가-힣])");
            Matcher m = block.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String token = m.group();
                try {
                    long val = parseSinoKorean(token);
                    m.appendReplacement(sb, String.valueOf(val));
                } catch (Exception ex) {
                    m.appendReplacement(sb, token);
                }
            }
            m.appendTail(sb);
            return sb.toString();
        }

        private static long parseSinoKorean(String token) {
            long total = 0;
            String[] eSplit = token.split("억", -1);
            if (eSplit.length > 2) throw new IllegalArgumentException("too many 억");
            if (eSplit.length == 2) {
                total += parseUnder10000(eSplit[0]) * 100_000_000L;
                token = eSplit[1];
            }

            String[] mSplit = token.split("만", -1);
            if (mSplit.length > 2) throw new IllegalArgumentException("too many 만");
            if (mSplit.length == 2) {
                total += parseUnder10000(mSplit[0]) * 10_000L;
                total += parseUnder10000(mSplit[1]);
            } else {
                total += parseUnder10000(token);
            }
            return total;
        }

        private static int parseUnder10000(String s) {
            if (s == null || s.isEmpty()) return 0;
            int acc = 0;
            int lastDigit = 0;
            int block = 0;
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (UNIT.containsKey(ch)) {
                    int unitVal = UNIT.get(ch);
                    block += (lastDigit == 0 ? 1 : lastDigit) * unitVal;
                    lastDigit = 0;
                } else {
                    lastDigit = DIGIT.getOrDefault(ch, 0);
                }
            }
            acc += block + lastDigit;
            return acc;
        }

        private static String normalizePhoneLikeSequencesWhenContext(String s) {
            String lower = s;
            boolean hasPhoneContext = PHONE_HINTS.stream().anyMatch(lower::contains);
            if (!hasPhoneContext) return s;

            Pattern seq = Pattern.compile("(?:\\b\\d\\b\\s*){9,12}");
            Matcher m = seq.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String joined = m.group().replaceAll("\\s+", "");
                String formatted = formatPhone(joined);
                m.appendReplacement(sb, formatted);
            }
            m.appendTail(sb);
            return sb.toString();
        }

        private static String formatPhone(String digits) {
            if (digits.startsWith("02") && (digits.length() == 9 || digits.length() == 10)) {
                if (digits.length() == 9) return digits.replaceFirst("^(02)(\\d{3})(\\d{4})$", "$1-$2-$3");
                return digits.replaceFirst("^(02)(\\d{4})(\\d{4})$", "$1-$2-$3");
            }
            if (digits.length() == 10) {
                return digits.replaceFirst("^(\\d{3})(\\d{3})(\\d{4})$", "$1-$2-$3");
            }
            if (digits.length() == 11) {
                return digits.replaceFirst("^(\\d{3})(\\d{4})(\\d{4})$", "$1-$2-$3");
            }
            return digits;
        }
    }

    private static final class AlphabetNormalizer {

        private static final Map<String, String> KOR_TO_ENG = Map.ofEntries(
                Map.entry("에이", "A"), Map.entry("비", "B"), Map.entry("씨", "C"),
                Map.entry("디", "D"), Map.entry("이", "E"), Map.entry("에프", "F"),
                Map.entry("지", "G"), Map.entry("에이치", "H"), Map.entry("아이", "I"),
                Map.entry("제이", "J"), Map.entry("케이", "K"), Map.entry("엘", "L"),
                Map.entry("엠", "M"), Map.entry("엔", "N"), Map.entry("오", "O"),
                Map.entry("피", "P"), Map.entry("큐", "Q"), Map.entry("알", "R"),
                Map.entry("에스", "S"), Map.entry("티", "T"), Map.entry("유", "U"),
                Map.entry("브이", "V"), Map.entry("더블유", "W"), Map.entry("엑스", "X"),
                Map.entry("와이", "Y"), Map.entry("지", "Z")
        );

        static String normalize(String input) {
            if (input == null || input.isBlank()) return input;

            String result = input;
            for (var entry : KOR_TO_ENG.entrySet()) {
                result = result.replaceAll("(?<![가-힣])" + entry.getKey() + "(?![가-힣])", entry.getValue());
            }

            result = result.replaceAll("(?<=\\b[A-Z])\\s+(?=[A-Z]\\b)", "");
            return result.trim();
        }
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