package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 서버 경로 검색 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchPathResponse {
    private String type;
    private String status;
    private SearchPathData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchPathData {
        private String query;

        @JsonProperty("total_matched")
        private Integer totalMatched;

        @JsonProperty("matched_paths")
        private List<MatchedPath> matchedPaths;

        private Performance performance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchedPath {
        private String domain;

        @JsonProperty("task_intent")
        private String taskIntent;

        @JsonProperty("relevance_score")
        private Double relevanceScore;

        private Integer weight;

        private List<StepResponse> steps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepResponse {
        private Integer order;

        private String url;

        private String action;

        private List<String> selectors;

        private String description;

        @JsonProperty("is_input")
        private Boolean isInput;

        @JsonProperty("input_type")
        private String inputType;

        @JsonProperty("input_placeholder")
        private String inputPlaceholder;

        @JsonProperty("should_wait")
        private Boolean shouldWait;

        @JsonProperty("wait_message")
        private String waitMessage;

        @JsonProperty("text_labels")
        private List<String> textLabels;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Performance {
        @JsonProperty("search_time")
        private Integer searchTime;
    }
}