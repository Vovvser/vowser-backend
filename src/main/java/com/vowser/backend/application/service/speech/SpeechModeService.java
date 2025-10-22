package com.vowser.backend.application.service.speech;

import com.vowser.backend.common.enums.SpeechMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
public class SpeechModeService {

    public EnumSet<SpeechMode> buildModes(
            boolean enableGeneralMode,
            boolean enableNumberMode,
            boolean enableAlphabetMode
    ) {
        EnumSet<SpeechMode> modes = EnumSet.noneOf(SpeechMode.class);
        
        if (enableGeneralMode) modes.add(SpeechMode.GENERAL);
        if (enableNumberMode) modes.add(SpeechMode.NUMBER);
        if (enableAlphabetMode) modes.add(SpeechMode.ALPHABET);

        log.info("Built speech recognition modes: {}", modes);
        return modes;
    }

    public List<String> buildPhraseHints(EnumSet<SpeechMode> modes, List<String> customPhrases) {
        List<String> phrases = new ArrayList<>();

        if (modes.contains(SpeechMode.NUMBER)) {
            phrases.addAll(getNumberPhrases());
        }
        if (modes.contains(SpeechMode.ALPHABET)) {
            phrases.addAll(getAlphabetPhrases());
        }
        if (customPhrases != null) {
            phrases.addAll(customPhrases);
        }

        log.debug("Built phrase hints: {} phrases", phrases.size());
        return phrases;
    }


    private List<String> getNumberPhrases() {
        return List.of("영", "공", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구",
                      "하나", "둘", "셋", "넷", "다섯", "여섯", "일곱", "여덟", "아홉", "열");
    }

    private List<String> getAlphabetPhrases() {
        return List.of(
                "에이","비","씨","디","이","에프","지","에이치","아이","제이","케이","엘",
                "엠","엔","오","피","큐","알","에스","티","유","브이","더블유","엑스","와이","지"
        );
    }
}
