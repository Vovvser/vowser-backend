package com.vowser.backend.api.dto.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 경로 저장 요청 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathSubmission {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("task_intent")
    private String taskIntent;

    private String domain;

    private List<StepData> steps;
}