package com.vowser.backend.application.service.speech;

import com.vowser.backend.common.enums.SpeechMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpeechModeServiceTest {

    private SpeechModeService speechModeService;

    @BeforeEach
    void setUp() {
        speechModeService = new SpeechModeService();
    }

    @Test
    @DisplayName("모든 모드가 비활성화된 경우 빈 EnumSet 반환")
    void buildModes_AllDisabled_ReturnsEmptySet() {
        EnumSet<SpeechMode> result = speechModeService.buildModes(
                false,
                false,
                false,
                false
        );

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("일반 모드만 활성화된 경우")
    void buildModes_OnlyGeneralEnabled_ReturnsGeneralMode() {
        EnumSet<SpeechMode> result = speechModeService.buildModes(
                true,
                false,
                false,
                false
        );

        assertEquals(1, result.size());
        assertTrue(result.contains(SpeechMode.GENERAL));
    }

    @Test
    @DisplayName("모든 모드가 활성화된 경우")
    void buildModes_AllEnabled_ReturnsAllModes() {
        EnumSet<SpeechMode> result = speechModeService.buildModes(
                true,
                true,
                true,
                true
        );

        assertEquals(4, result.size());
        assertTrue(result.contains(SpeechMode.GENERAL));
        assertTrue(result.contains(SpeechMode.NUMBER));
        assertTrue(result.contains(SpeechMode.ALPHABET));
        assertTrue(result.contains(SpeechMode.SNIPPET));
    }

    @Test
    @DisplayName("숫자 모드가 포함된 경우 숫자 phrase hints 포함")
    void buildPhraseHints_NumberModeEnabled_ContainsNumberPhrases() {
        EnumSet<SpeechMode> modes = EnumSet.of(SpeechMode.NUMBER);
        List<String> customPhrases = new ArrayList<>();

        List<String> result = speechModeService.buildPhraseHints(modes, customPhrases);

        assertTrue(result.contains("영"));
        assertTrue(result.contains("일"));
        assertTrue(result.contains("하나"));
        assertTrue(result.contains("둘"));
    }

    @Test
    @DisplayName("커스텀 phrase가 포함된 경우")
    void buildPhraseHints_WithCustomPhrases_ContainsCustomPhrases() {
        EnumSet<SpeechMode> modes = EnumSet.noneOf(SpeechMode.class);
        List<String> customPhrases = List.of("커스텀1", "커스텀2", "테스트");

        List<String> result = speechModeService.buildPhraseHints(modes, customPhrases);

        assertTrue(result.contains("커스텀1"));
        assertTrue(result.contains("커스텀2"));
        assertTrue(result.contains("테스트"));
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("null 커스텀 phrase 처리")
    void buildPhraseHints_NullCustomPhrases_DoesNotThrowException() {
        EnumSet<SpeechMode> modes = EnumSet.of(SpeechMode.GENERAL);

        assertDoesNotThrow(() -> {
            List<String> result = speechModeService.buildPhraseHints(modes, null);
            assertNotNull(result);
        });
    }
}