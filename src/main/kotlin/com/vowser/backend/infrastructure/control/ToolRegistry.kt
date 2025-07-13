package com.vowser.backend.infrastructure.control

import com.vowser.backend.infrastructure.control.tool.BrowserTool
import org.springframework.stereotype.Component

@Component
class ToolRegistry(tools: List<BrowserTool<*>>) {
    private val toolMap: Map<String, BrowserTool<*>> = tools.associateBy { it.name }

    fun getTool(name: String): BrowserTool<*>? {
        return toolMap[name]
    }
}