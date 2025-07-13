package com.vowser.backend.infrastructure.control.tool

import com.vowser.backend.api.dto.ClickArgs
import com.vowser.backend.api.dto.TextContent
import com.vowser.backend.api.dto.ToolResult
import com.vowser.backend.application.service.ControlService
import org.springframework.stereotype.Component

@Component
class ClickTool(
    private val controlService: ControlService
) : BrowserTool<ClickArgs> {

    override val name = "clickElement"
    override val argumentType = ClickArgs::class.java

    override fun execute(args: ClickArgs): ToolResult {
        val command = mapOf("action" to "click", "selector" to args.elementId)
        controlService.sendCommandToClient(command)
        return ToolResult(listOf(TextContent(text = "Clicked '${args.elementId}'")))
    }
}