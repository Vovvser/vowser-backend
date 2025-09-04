package com.vowser.backend.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Vowser Backend API",
                version = "v1.0.0",
                description = """
            Voice-driven web automation service API

            The Central Communication & Control Hub for the Vowser Ecosystem.
            This API provides endpoints for:
            - Browser control and navigation
            - Speech-to-text processing
            - Real-time WebSocket communication
            - MCP server integration
            """,
                contact = @Contact(
                        name = "Vowser Team",
                        url = "https://github.com/Vovvser"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        )
)
public class OpenApiConfig {
}