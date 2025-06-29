package com.vowser.backend.infrastructure.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Vowser Backend API",
        version = "v1",
        description = "Voice‑driven web automation service"
    )
)
class OpenApiConfig