package com.vowser.backend.api.dto

// --- 요청(Request) DTO ---
data class CallToolRequest(
    val toolName: String,
    val args: Map<String, Any>
)

// --- 응답(Response) DTO ---
data class ToolResult(
    val content: List<Content>,
    val isError: Boolean = false
)

sealed class Content
data class TextContent(val type: String = "text", val text: String) : Content()
data class ImageContent(val type: String = "image", val data: String, val mimeType: String) : Content()

// --- 각 Tool의 인자(Args) DTO ---
data class ClickArgs(val elementId: String)
data class GoBackArgs(val placeholder: String? = null)