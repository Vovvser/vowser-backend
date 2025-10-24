package com.vowser.backend.application.service.speech;

import com.vowser.backend.common.constants.NetworkConstants;
import com.vowser.backend.common.enums.SpeechMode;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.common.exception.SpeechException;
import java.nio.charset.StandardCharsets;
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
                result = AlphabetNormalizer.normalize(result); // ← 추가
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

    static final class NumberNormalizer {
        private NumberNormalizer(){}

        private static final String WORD = "[가-힣A-Za-z0-9]";

        private static final Map<String, String> LONG_DIGITS = new LinkedHashMap<>();
        static {
            LONG_DIGITS.put("제로","0");
            LONG_DIGITS.put("하나","1");
            LONG_DIGITS.put("둘","2");
            LONG_DIGITS.put("셋","3");
            LONG_DIGITS.put("넷","4");
            LONG_DIGITS.put("다섯","5");
            LONG_DIGITS.put("여섯","6");
            LONG_DIGITS.put("일곱","7");
            LONG_DIGITS.put("여덟","8");
            LONG_DIGITS.put("아홉","9");
        }

        private static final Map<String, String> MONO_DIGITS = new LinkedHashMap<>();
        static {
            MONO_DIGITS.put("공","0"); MONO_DIGITS.put("영","0");
            MONO_DIGITS.put("일","1"); MONO_DIGITS.put("이","2");
            MONO_DIGITS.put("삼","3"); MONO_DIGITS.put("사","4");
            MONO_DIGITS.put("오","5"); MONO_DIGITS.put("육","6");
            MONO_DIGITS.put("칠","7"); MONO_DIGITS.put("팔","8");
            MONO_DIGITS.put("구","9");
        }

        private static final Map<Character, Integer> UNIT = Map.of('십',10,'백',100,'천',1000);
        private static final Map<Character, Long> BIG = Map.of(
                '만', 10_000L,
                '억', 100_000_000L,
                '조', 1_000_000_000_000L
        );

        private static final List<String> PHONE_HINTS = Arrays.asList(
                "전화","번호","연락처","휴대폰","핸드폰","대표번호","팩스","ars","콜센터","문의"
        );

        static String normalize(String input) {
            if (input == null || input.isBlank()) return input;

            String s = input;

            s = replaceDigitsWithContext(s);

            s = replaceSinoKoreanNumbers(s);

            s = s.replaceAll("(\\d)\\s*점\\s*(\\d+)", "$1.$2");
            s = normalizePhoneLikeSequencesWhenContext(s);
            s = s.replaceAll("(?<=\\d)\\s+(?=\\d)", "");
            s = s.replaceAll("\\s+", " ").trim();
            return s;
        }

        private static String replaceDigitsWithContext(String s) {
            String[] raw = s.split("(?<=\\p{Punct}|\\s)|(?=\\p{Punct}|\\s)");
            List<String> out = new ArrayList<>(raw.length);

            final Set<String> NUM_EXPECTING_NEXT = Set.of(
                    "자리","층","번","호","호선","학년","차","호실","월","일","년",
                    "페이지","버전","점","차수","회","동","시","분","초","호수"
            );

            for (int i = 0; i < raw.length; i++) {
                String t = raw[i];
                String next = (i+1 < raw.length) ? raw[i+1] : "";
                String nextCore = next.replaceAll("[^가-힣A-Za-z0-9]", "");

                String replaced = replaceTokenIfNumberWord(t, NUM_EXPECTING_NEXT.contains(nextCore));
                out.add(replaced);
            }
            return String.join("", out);
        }

        private static String replaceTokenIfNumberWord(String token, boolean allowMonosyllable) {
            String core = token.replaceAll("^" + WORD + "|" + WORD + "$", "");
            for (var e : LONG_DIGITS.entrySet()) {
                if (token.equals(e.getKey())) return e.getValue();
            }
            if (allowMonosyllable && MONO_DIGITS.containsKey(token)) {
                return MONO_DIGITS.get(token);
            }
            return token;
        }

        private static String replaceSinoKoreanNumbers(String s) {
            Pattern block = Pattern.compile("(?<![가-힣])([영공일이삼사오육칠팔구십백천만억조]{2,})(?![가-힣])");
            Matcher m = block.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String token = m.group(1);
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
            long total = 0L;

            String[] byJo = token.split("조", -1);
            if (byJo.length > 2) throw new IllegalArgumentException("too many 조");
            if (byJo.length == 2) {
                total += parseUnder10000(byJo[0]) * BIG.get('조');
                token = byJo[1];
            }

            String[] byEok = token.split("억", -1);
            if (byEok.length > 2) throw new IllegalArgumentException("too many 억");
            if (byEok.length == 2) {
                total += parseUnder10000(byEok[0]) * BIG.get('억');
                token = byEok[1];
            }

            String[] byMan = token.split("만", -1);
            if (byMan.length > 2) throw new IllegalArgumentException("too many 만");
            if (byMan.length == 2) {
                total += parseUnder10000(byMan[0]) * BIG.get('만');
                total += parseUnder10000(byMan[1]);
            } else {
                total += parseUnder10000(token);
            }
            return total;
        }

        private static int parseUnder10000(String s) {
            if (s == null || s.isEmpty()) return 0;
            int block = 0, lastDigit = 0;
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (UNIT.containsKey(ch)) {
                    int unitVal = UNIT.get(ch);
                    block += (lastDigit == 0 ? 1 : lastDigit) * unitVal;
                    lastDigit = 0;
                } else {
                    lastDigit = monoDigitValue(ch);
                }
            }
            return block + lastDigit;
        }

        private static int monoDigitValue(char ch) {
            switch (ch) {
                case '영': case '공': return 0;
                case '일': return 1;
                case '이': return 2;
                case '삼': return 3;
                case '사': return 4;
                case '오': return 5;
                case '육': return 6;
                case '칠': return 7;
                case '팔': return 8;
                case '구': return 9;
                default: return 0;
            }
        }

        private static String normalizePhoneLikeSequencesWhenContext(String s) {
            String lower = s.toLowerCase(Locale.ROOT);
            boolean hasPhoneContext = PHONE_HINTS.stream().anyMatch(lower::contains);
            if (!hasPhoneContext) return s;

            Pattern seq = Pattern.compile("(?:\\b\\d\\b\\s*){9,12}|(?:\\+?\\d[\\s-]?){10,15}");
            Matcher m = seq.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String joined = m.group().replaceAll("[^\\d+]", "");
                String formatted = formatPhone(joined);
                m.appendReplacement(sb, formatted);
            }
            m.appendTail(sb);
            return sb.toString();
        }

        private static String formatPhone(String digits) {
            if (digits.startsWith("+82")) {
                digits = digits.replaceFirst("^\\+?82", "0");
            }
            if (digits.startsWith("02") && (digits.length() == 9 || digits.length() == 10)) {
                return digits.length() == 9
                        ? digits.replaceFirst("^(02)(\\d{3})(\\d{4})$", "$1-$2-$3")
                        : digits.replaceFirst("^(02)(\\d{4})(\\d{4})$", "$1-$2-$3");
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

        private static final String WORD = "[가-힣A-Za-z0-9]";

        private static final Map<String, String> KOR_TO_ENG_SAFE = Map.ofEntries(
                Map.entry("에이", "A"),
                Map.entry("비", "B"),
                Map.entry("씨", "C"),
                Map.entry("디", "D"),
                Map.entry("이", "E"),
                Map.entry("에프", "F"),
                Map.entry("지", "G"),
                Map.entry("에이치", "H"),
                Map.entry("에취", "H"),
                Map.entry("아이", "I"),
                Map.entry("제이", "J"),
                Map.entry("케이", "K"),
                Map.entry("엘", "L"),
                Map.entry("엠", "M"),
                Map.entry("엔", "N"),
                Map.entry("오", "O"),
                Map.entry("피", "P"),
                Map.entry("큐", "Q"),
                Map.entry("아르", "R"),
                Map.entry("에스", "S"),
                Map.entry("티", "T"),
                Map.entry("유", "U"),
                Map.entry("브이", "V"),
                Map.entry("더블유", "W"),
                Map.entry("더블류", "W"),
                Map.entry("엑스", "X"),
                Map.entry("엑쓰", "X"),
                Map.entry("와이", "Y"),
                Map.entry("제트", "Z")
        );

        private static final Set<String> MONO_RISK = Set.of("이", "오", "알");

        static String normalize(String input) {
            if (input == null || input.isBlank()) return input;
            String s = input;

            s = replaceBySafeTokensWithBoundary(s);

            s = joinLetterRuns(s);

            return s.trim();
        }

        private static String replaceBySafeTokensWithBoundary(String s) {
            String[] raw = s.split("(?<=\\p{Punct}|\\s)|(?=\\p{Punct}|\\s)");
            List<String> out = new ArrayList<>(raw.length);

            for (int i = 0; i < raw.length; i++) {
                String t = raw[i];
                String core = t.replaceAll("^" + WORD + "|" + WORD + "$", ""); // 선택적 보조(경계)
                String prev = (i > 0) ? raw[i-1] : "";
                String next = (i+1 < raw.length) ? raw[i+1] : "";

                String repl = tryMapToken(t, prev, next);
                out.add(repl);
            }
            return String.join("", out);
        }

        private static String tryMapToken(String token, String prev, String next) {
            String key = token;

            String mapped = KOR_TO_ENG_SAFE.get(key);
            if (mapped == null) {
                return token;
            }

            if (MONO_RISK.contains(key)) {
                if (!looksLikeAlphabetContext(prev, next)) {
                    return token;
                }
            }

            return mapped;
        }

        private static boolean looksLikeAlphabetContext(String prev, String next) {
            String p = prev.strip();
            String n = next.strip();
            boolean prevIsSep = p.isEmpty() || p.matches("\\p{Punct}+");
            boolean nextIsSep = n.isEmpty() || n.matches("\\p{Punct}+");
            boolean prevIsAlphaWord = KOR_TO_ENG_SAFE.containsKey(p);
            boolean nextIsAlphaWord = KOR_TO_ENG_SAFE.containsKey(n);
            return (prevIsSep || prevIsAlphaWord) && (nextIsSep || nextIsAlphaWord);
        }

        private static String joinLetterRuns(String s) {
            String prev;
            String cur = s;
            do {
                prev = cur;
                cur = cur.replaceAll("(?<=\\b[A-Z])\\s+(?=[A-Z]\\b)", "");
                cur = cur.replaceAll("(?<=\\b[A-Z])\\s*[,·•\\-]\\s*(?=[A-Z]\\b)", "");
            } while (!cur.equals(prev));
            return cur;
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