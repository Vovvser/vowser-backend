package com.vowser.backend.infrastructure.control.tool

import com.vowser.backend.api.dto.GoBackArgs
import com.vowser.backend.api.dto.TextContent
import com.vowser.backend.api.dto.ToolResult

import com.vowser.backend.application.service.ControlService
import org.springframework.stereotype.Component

@Component
class GoBackTool(
    private val controlService: ControlService
) : BrowserTool<GoBackArgs> {

    override val name: String = "goBack"
    override val argumentType = GoBackArgs::class.java

    override fun execute(args: GoBackArgs): ToolResult {
        val command = mapOf("action" to "goBack")
        controlService.sendCommandToClient(command)
        return ToolResult(listOf(TextContent(text = "'뒤로가기' 명령을 클라이언트에 전송했습니다.")))
    }
}