package com.vowser.backend.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// 클라이언트로 보낼 최상위 응답 객체
data class AllPathsResponse(
    val query: String,
    val paths: List<PathDetail>
)

// 상세 정보를 포함한 개별 경로 DTO
data class PathDetail(
    val pathId: String,
    val score: Double?,
    val total_weight: Int?,
    val last_used: String?,
    val estimated_time: Double?,
    val steps: List<NavigationStep>
)

// 경로의 한 단계를 나타내는 DTO
data class NavigationStep(
    val url: String,
    val title: String,
    val action: String, // "navigate", "type", "click", etc.
    val selector: String? = null,
    val htmlAttributes: Map<String, Any>? = null
)

data class NavigationPathResponse(
    val message: String,
    val pathId: String,
    val stepCount: Int
)

// --- MCP 서버 응답 파싱을 위한 DTO ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class McpSearchPathResult(
    val type: String,
    val status: String,
    val data: McpSearchPathData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class McpSearchPathData(
    val query: String,
    val matched_paths: List<McpMatchedPath>? = null,
    val message: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class McpMatchedPath(
    val pathId: String,
    val score: Double?,
    val total_weight: Int?,
    val last_used: String?,
    val estimated_time: Double?,
    val steps: List<McpStep>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class McpStep(
    val title: String,
    val action: String,
    val url: String,
    val selector: String? = null
)