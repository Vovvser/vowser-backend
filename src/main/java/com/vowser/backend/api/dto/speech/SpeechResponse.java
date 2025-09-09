package com.vowser.backend.api.dto.speech;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 음성 → 텍스트 처리 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechResponse {
    private boolean success;
    private String transcript;
    private String message;
}