package com.vowser.backend.application.service.speech.normalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AlphabetNormalizer {

    private AlphabetNormalizer() {}

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

    public static String normalize(String input) {
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
            String prev = (i > 0) ? raw[i - 1] : "";
            String next = (i + 1 < raw.length) ? raw[i + 1] : "";

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
            cur = cur.replaceAll("(?<=\\b[A-Z])\\s*[,·•-]\\s*(?=[A-Z]\\b)", "");
        } while (!cur.equals(prev));
        return cur;
    }
}
