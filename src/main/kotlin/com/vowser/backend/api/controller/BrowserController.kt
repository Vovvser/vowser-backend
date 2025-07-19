package com.vowser.backend.api.controller

import com.vowser.backend.application.service.ControlService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/browser-control")
class BrowserController(
    private val controlService: ControlService
) {

    @GetMapping("/navigate")
    fun navigate(@RequestParam url: String): Map<String, String> {
        val command = mapOf(
            "type" to "com.vowser.client.websocket.dto.BrowserCommand.Navigate",
            "url" to url
        )
        controlService.sendCommandToClient(command)
        return mapOf("message" to "Navigate command sent to client with URL: $url")
    }

    @GetMapping("/go-back")
    fun goBack(): Map<String, String> {
        val command = mapOf(
            "type" to "com.vowser.client.websocket.dto.BrowserCommand.GoBack"
        )
        controlService.sendCommandToClient(command)
        return mapOf("message" to "GoBack command sent to client.")
    }

    @GetMapping("/go-forward")
    fun goForward(): Map<String, String> {
        val command = mapOf(
            "type" to "com.vowser.client.websocket.dto.BrowserCommand.GoForward"
        )
        controlService.sendCommandToClient(command)
        return mapOf("message" to "GoForward command sent to client.")
    }
}