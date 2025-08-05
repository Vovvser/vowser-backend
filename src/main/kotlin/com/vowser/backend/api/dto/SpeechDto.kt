package com.vowser.backend.api.dto

data class SpeechResponse(
    val success: Boolean,
    val transcript: String?,
    val message: String? = null
)