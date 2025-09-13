package com.vowser.backend.application.service.speech;

import com.vowser.backend.common.enums.SpeechMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSpeechDirectTest {

    private final SpeechModeService speechModeService = new SpeechModeService();

    private record TestCase(int fileNumber, String originalText, String expectedResult) {}

    private static Stream<TestCase> cases() {
        return Stream.of(
                new TestCase(1,  "일 이 삼 사 오",                       "1 2 3 4 5"),
                new TestCase(2,  "하나 둘 셋 넷 다섯",                   "1 2 3 4 5"),
                new TestCase(3,  "전화번호는 공일공 이삼사오 육칠팔구입니다", "전화번호는 010 2345 6789입니다"),
                new TestCase(4,  "일곱 더하기 삼은 열이다",               "7 더하기 3은 10이다"),
                new TestCase(5,  "비밀번호는 영일영이공사입니다",          "비밀번호는 010204입니다"),
                new TestCase(6,  "서울시 강남구 삼성동 일육구번지",       "서울시 강남구 삼성동 169번지"),
                new TestCase(7,  "오후 세시 이십오분",                    "오후 3시 25분"),
                new TestCase(8,  "사과 여섯개랑 배 아홉개 주세요",         "사과 6개랑 배 9개 주세요"),
                new TestCase(9,  "일학년 이반 칠번 김하나입니다",          "1학년 2반 7번 김1입니다"),
                new TestCase(10, "영일이삼사오육칠팔구",                  "0123456789")
        );
    }

    @ParameterizedTest(name = "wav 로딩 #{0}")
    @MethodSource("cases")
    @DisplayName("10개 wav 파일이 클래스패스에서 정상 로딩되어야 한다")
    void testAudioFileLoading_withAssertions(TestCase tc) throws IOException {
        MockMultipartFile file = loadAudioFile(tc.fileNumber());

        assertAll(
                () -> assertNotNull(file, "MockMultipartFile은 null이면 안 됨"),
                () -> assertEquals("sentence_" + tc.fileNumber() + ".wav", file.getOriginalFilename()),
                () -> assertTrue(file.getSize() > 0, "오디오 파일 크기가 0보다 커야 함"),
                () -> assertEquals("audio/wav", file.getContentType(), "MIME 타입이 audio/wav 여야 함")
        );
    }

    @Test
    @DisplayName("NUMBER 모드에서 숫자 phrase hints가 비어있지 않고 핵심 항목을 포함해야 한다")
    void testPhraseHintsEffect_withAssertions() {
        EnumSet<SpeechMode> numberMode = EnumSet.of(SpeechMode.NUMBER);
        List<String> hints = speechModeService.buildPhraseHints(numberMode, null);

        assertAll(
                () -> assertNotNull(hints, "phrase hints 리스트는 null이면 안 됨"),
                () -> assertFalse(hints.isEmpty(), "phrase hints는 비어있으면 안 됨"),
                () -> assertTrue(hints.containsAll(List.of("영","공","일","이","삼","사","오","육","칠","팔","구")),
                        "한자수 단어가 모두 포함되어야 함"),
                () -> assertTrue(hints.containsAll(List.of("하나","둘","셋","넷","다섯","여섯","일곱","여덟","아홉","열")),
                        "순우리말 수 단어가 모두 포함되어야 함")
        );
    }

    // 통합 테스트 예시 (실제 GCP 호출). 빌드 파이프라인에서는 제외하는것 추천
    // @Test
    // @Tag("integration")
    // @DisplayName("실제 Google Speech API 호출로 transcribe가 문자열을 반환해야 한다")
    // void transcribe_integration() { ... }

    private MockMultipartFile loadAudioFile(int fileNumber) throws IOException {
        String path = "sound/sentence_" + fileNumber + ".wav";
        ClassPathResource resource = new ClassPathResource(path);

        assertTrue(resource.exists(), "리소스가 클래스패스에 존재해야 함: " + path);

        return new MockMultipartFile(
                "audio",
                "sentence_" + fileNumber + ".wav",
                "audio/wav",
                resource.getInputStream().readAllBytes()
        );
    }
}