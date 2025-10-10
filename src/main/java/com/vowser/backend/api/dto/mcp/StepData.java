package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 경로 내 개별 단계 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepData {

    private String url;
    private String domain;

    private List<String> selectors;

    @JsonProperty("anchor_point")
    private String anchorPoint;

    @JsonProperty("relative_path_from_anchor")
    private String relativePathFromAnchor;

    private String action;

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

    @JsonProperty("max_wait_time")
    private Integer maxWaitTime;

    private String description;

    @JsonProperty("text_labels")
    private List<String> textLabels;

    @JsonProperty("context_text")
    private String contextText;

    @JsonProperty("success_rate")
    private Double successRate;
}