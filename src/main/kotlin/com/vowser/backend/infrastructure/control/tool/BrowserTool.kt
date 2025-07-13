package com.vowser.backend.infrastructure.control.tool

import com.vowser.backend.api.dto.ToolResult

interface BrowserTool<in T : Any> {
    val name: String
    val argumentType: Class<out @UnsafeVariance T>
    fun execute(args: T): ToolResult
}