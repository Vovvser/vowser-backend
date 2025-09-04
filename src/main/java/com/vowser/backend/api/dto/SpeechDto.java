package com.vowser.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 음성 관련 데이터 전송 객체
 *
 * 음성 → 텍스트 변환 및 음성 명령 처리를 위한 DTO
 */
public class SpeechDto {

    /**
     * 음성 → 텍스트 처리 응답
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeechResponse {
        private boolean success;
        private String transcript;
        private String message;

    }
}