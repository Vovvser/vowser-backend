package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 서버 그래프 통계 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphStatsResponse {
    private String type;
    private String status;
    private GraphStatsData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphStatsData {
        private String message;

        @JsonProperty("node_counts")
        private Map<String, Integer> nodeCounts;

        @JsonProperty("relationship_count")
        private Integer relationshipCount;
    }
}