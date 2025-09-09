package com.vowser.backend.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpeechMode {

    GENERAL("GENERAL", "일반 음성 인식"),
    NUMBER("NUMBER", "숫자 인식 최적화"),
    ALPHABET("ALPHABET", "알파벳 인식 최적화"),
    SNIPPET("SNIPPET", "코드/명령어 인식");

    private final String code;
    private final String description;

    @Override
    public String toString() {
        return code;
    }

    public static SpeechMode fromCode(String code) {
        for (SpeechMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown SpeechMode code: " + code);
    }
}
