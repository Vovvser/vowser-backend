package com.vowser.backend.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class NavigationPathRequest(
    val pathId: String,
    val description: String,
    val steps: List<NavigationStep>
)

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
    val matched_paths: List<McpMatchedPath>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class McpMatchedPath(
    val pathId: String,
    val relevance_score: Double?,
    val total_weight: Int?,
    val last_used: String?,
    val estimated_time: Double?, // JSON에서 null일 수 있으므로 Nullable
    val steps: List<McpStep>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class McpStep(
    val order: Int,
    val type: String,
    val title: String,
    val url: String,
    val action: String,
    val domain: String? = null,
    val pageId: String? = null,
    val selector: String? = null,
    val anchorPoint: String? = null,
    val textLabels: List<String>? = null
)