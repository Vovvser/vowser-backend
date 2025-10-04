package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 서버 경로 저장 응답
 *
 * db-refactor 마이그레이션: 새로운 구조
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SavePathResponse {
    private String type;
    private String status;
    private SavePathData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavePathData {
        private String message;
        private SavePathResult result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavePathResult {
        private String status;

        private String domain;

        @JsonProperty("task_intent")
        private String taskIntent;

        @JsonProperty("steps_saved")
        private Integer stepsSaved;
    }
}