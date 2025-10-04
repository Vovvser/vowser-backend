package com.vowser.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 네비게이션 관련 데이터 전송 객체
 *
 * 브라우저 네비게이션, 경로 탐색,
 * MCP 서버와의 경로 검색 통신을 위한 DTO
 */
public class NavigationDto {

    /**
     * 특정 쿼리에 대한 모든 네비게이션 경로를 담는 최상위 응답
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllPathsResponse {
        private String query;
        private List<PathDetail> paths;
    }

    /**
     * 단일 네비게이션 경로의 상세 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathDetail {
        private String pathId;
        private Double score;
        
        @JsonProperty("total_weight")
        private Integer totalWeight;
        
        private String lastUsed;
        private Double estimatedTime;
        private List<NavigationStep> steps;
    }

    /**
     * 네비게이션 경로 내 개별 단계
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationStep {
        private String url;
        private String title;
        private String action;
        private String selector;
        private Map<String, Object> htmlAttributes;

    }

    /**
     * 네비게이션 경로 관련 작업의 응답
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationPathResponse {
        private String message;
        private String pathId;
        private int stepCount;
    }

    /**
     * MCP 서버에서 반환하는 경로 검색 최상위 응답
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpSearchPathResult {
        private String type;
        private String status;
        private McpSearchPathData data;
    }

    /**
     * MCP 경로 검색 응답의 데이터 부분
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpSearchPathData {
        private String query;
        
        @JsonProperty("matched_paths")
        private List<McpMatchedPath> matchedPaths;
        
        private String message;
    }

    /**
     * MCP 서버에서 반환된 개별 매칭 경로
     *
     * @deprecated Use com.vowser.backend.api.dto.mcp.SearchPathResponse.MatchedPath instead (db-refactor)
     */
    @Deprecated
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpMatchedPath {
        private String pathId;
        private Double score;

        @JsonProperty("total_weight")
        private Integer totalWeight;

        private String lastUsed;
        private Double estimatedTime;
        private List<McpStep> steps;
    }

    /**
     * taskIntent 기반 검색 구조
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpMatchedPathNew {
        private String domain;

        @JsonProperty("task_intent")
        private String taskIntent;

        @JsonProperty("relevance_score")
        private Double relevanceScore;

        private Integer weight;

        private List<McpStepNew> steps;
    }

    /**
     * MCP 서버 응답 내 개별 단계
     *
     * @deprecated Use com.vowser.backend.api.dto.mcp.SearchPathResponse.StepResponse instead (db-refactor)
     */
    @Deprecated
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpStep {
        private String title;
        private String action;
        private String url;
        private String selector;
    }

    /**
     * 다중 셀렉터, 입력/대기 지원
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpStepNew {
        private Integer order;
        private String url;
        private String action;  // "click" | "input" | "wait"
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
}