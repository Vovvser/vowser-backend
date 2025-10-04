package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 서버 인기 경로 조회 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PopularPathsResponse {
    private String type;
    private String status;
    private PopularPathsData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PopularPathsData {
        private String domain;

        @JsonProperty("popular_paths")
        private List<PopularPath> popularPaths;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PopularPath {
        @JsonProperty("task_intent")
        private String taskIntent;

        private Integer weight;

        @JsonProperty("step_count")
        private Integer stepCount;
    }
}