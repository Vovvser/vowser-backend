package com.vowser.backend.api.controller

import com.vowser.backend.application.service.ControlService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Browser Control", description = "WebSocket을 통한 실시간 브라우저 제어 API")
@RestController
@RequestMapping("/browser-control")
class BrowserController(
    private val controlService: ControlService
) {

    @Operation(
        summary = "브라우저 페이지 이동",
        description = "WebSocket으로 연결된 클라이언트의 브라우저를 지정된 URL로 이동시킵니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "네비게이션 명령이 성공적으로 전송됨",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class),
                    examples = [ExampleObject(
                        value = """{"message": "Navigate command sent to client with URL: https://google.com"}"""
                    )]
                )]
            )
        ]
    )
    @GetMapping("/navigate")
    fun navigate(
        @Parameter(
            description = "이동할 URL (예: https://google.com)",
            required = true,
            example = "https://google.com"
        )
        @RequestParam url: String
    ): Map<String, String> {
        val command = mapOf(
            "type" to "com.vowser.client.websocket.dto.BrowserCommand.Navigate",
            "url" to url
        )
        controlService.sendCommandToClient(command)
        return mapOf("message" to "Navigate command sent to client with URL: $url")
    }

    @Operation(
        summary = "브라우저 뒤로가기",
        description = "WebSocket으로 연결된 클라이언트의 브라우저에서 이전 페이지로 이동합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "뒤로가기 명령이 성공적으로 전송됨",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class),
                    examples = [ExampleObject(
                        value = """{"message": "GoBack command sent to client."}"""
                    )]
                )]
            )
        ]
    )
    @GetMapping("/go-back")
    fun goBack(): Map<String, String> {
        val command = mapOf(
            "type" to "com.vowser.client.websocket.dto.BrowserCommand.GoBack"
        )
        controlService.sendCommandToClient(command)
        return mapOf("message" to "GoBack command sent to client.")
    }

    @Operation(
        summary = "브라우저 앞으로가기",
        description = "WebSocket으로 연결된 클라이언트의 브라우저에서 다음 페이지로 이동합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "앞으로가기 명령이 성공적으로 전송됨",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class),
                    examples = [ExampleObject(
                        value = """{"message": "GoForward command sent to client."}"""
                    )]
                )]
            )]
    )
    @GetMapping("/go-forward")
    fun goForward(): Map<String, String> {
        val command = mapOf(
            "type" to "com.vowser.client.websocket.dto.BrowserCommand.GoForward"
        )
        controlService.sendCommandToClient(command)
        return mapOf("message" to "GoForward command sent to client.")
    }
}