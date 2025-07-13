package com.vowser.backend.infrastructure.config

import com.vowser.backend.infrastructure.control.ControlWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val controlWebSocketHandler: ControlWebSocketHandler
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(controlWebSocketHandler, "/control")
            .setAllowedOrigins("*") // 모든 출처에서의 연결을 허용 (개발용)
    }
}