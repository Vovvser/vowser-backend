package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias("anchorPoint")
    private String anchorPoint;

    @JsonProperty("relative_path_from_anchor")
    @JsonAlias("relativePathFromAnchor")
    private String relativePathFromAnchor;

    private String action;

    @JsonProperty("is_input")
    @JsonAlias("isInput")
    private Boolean isInput;

    @JsonProperty("input_type")
    @JsonAlias("inputType")
    private String inputType;

    @JsonProperty("input_placeholder")
    @JsonAlias("inputPlaceholder")
    private String inputPlaceholder;

    @JsonProperty("should_wait")
    @JsonAlias("shouldWait")
    private Boolean shouldWait;

    @JsonProperty("wait_message")
    @JsonAlias("waitMessage")
    private String waitMessage;

    @JsonProperty("max_wait_time")
    @JsonAlias("maxWaitTime")
    private Integer maxWaitTime;

    private String description;

    @JsonProperty("text_labels")
    @JsonAlias("textLabels")
    private List<String> textLabels;

    @JsonProperty("context_text")
    @JsonAlias("contextText")
    private String contextText;

    @JsonProperty("success_rate")
    @JsonAlias("successRate")
    private Double successRate;
}
