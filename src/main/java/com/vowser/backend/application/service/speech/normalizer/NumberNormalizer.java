package com.vowser.backend.application.service.speech.normalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NumberNormalizer {
    private NumberNormalizer() {}

    private static final String WORD = "[가-힣A-Za-z0-9]";

    private static final Map<String, String> LONG_DIGITS = new LinkedHashMap<>();
    static {
        LONG_DIGITS.put("제로", "0");
        LONG_DIGITS.put("하나", "1");
        LONG_DIGITS.put("둘", "2");
        LONG_DIGITS.put("셋", "3");
        LONG_DIGITS.put("넷", "4");
        LONG_DIGITS.put("다섯", "5");
        LONG_DIGITS.put("여섯", "6");
        LONG_DIGITS.put("일곱", "7");
        LONG_DIGITS.put("여덟", "8");
        LONG_DIGITS.put("아홉", "9");
    }

    private static final Map<String, String> MONO_DIGITS = new LinkedHashMap<>();
    static {
        MONO_DIGITS.put("공", "0");
        MONO_DIGITS.put("영", "0");
        MONO_DIGITS.put("일", "1");
        MONO_DIGITS.put("이", "2");
        MONO_DIGITS.put("삼", "3");
        MONO_DIGITS.put("사", "4");
        MONO_DIGITS.put("오", "5");
        MONO_DIGITS.put("육", "6");
        MONO_DIGITS.put("칠", "7");
        MONO_DIGITS.put("팔", "8");
        MONO_DIGITS.put("구", "9");
    }

    private static final Map<Character, Integer> UNIT = Map.of('십', 10, '백', 100, '천', 1000);
    private static final Map<Character, Long> BIG = Map.of(
            '만', 10_000L,
            '억', 100_000_000L,
            '조', 1_000_000_000_000L
    );

    private static final List<String> PHONE_HINTS = Arrays.asList(
            "전화", "번호", "연락처", "휴대폰", "핸드폰", "대표번호", "팩스", "ars", "콜센터", "문의"
    );

    public static String normalize(String input) {
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
                "자리", "층", "번", "호", "호선", "학년", "차", "호실", "월", "일", "년",
                "페이지", "버전", "점", "차수", "회", "동", "시", "분", "초", "호수"
        );

        for (int i = 0; i < raw.length; i++) {
            String t = raw[i];
            String next = (i + 1 < raw.length) ? raw[i + 1] : "";
            String nextCore = next.replaceAll("[^가-힣A-Za-z0-9]", "");

            String replaced = replaceTokenIfNumberWord(t, NUM_EXPECTING_NEXT.contains(nextCore));
            out.add(replaced);
        }
        return String.join("", out);
    }

    private static String replaceTokenIfNumberWord(String token, boolean allowMonosyllable) {
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
            case '영':
            case '공':
                return 0;
            case '일':
                return 1;
            case '이':
                return 2;
            case '삼':
                return 3;
            case '사':
                return 4;
            case '오':
                return 5;
            case '육':
                return 6;
            case '칠':
                return 7;
            case '팔':
                return 8;
            case '구':
                return 9;
            default:
                return 0;
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
