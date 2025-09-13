package com.vowser.backend.api.dto.speech;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 음성 → 텍스트 처리 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechResponse {
    private boolean success;
    private String transcript;
    private String message;

    private List<String> requestedModes;
    private List<String> appliedModes;
}