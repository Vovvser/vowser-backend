package com.vowser.backend.api.dto

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