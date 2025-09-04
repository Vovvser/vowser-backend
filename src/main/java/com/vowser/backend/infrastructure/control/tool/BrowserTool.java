package com.vowser.backend.infrastructure.control.tool;

import com.vowser.backend.api.dto.ControlDto;

/**
 * Browser Tool Interface
 * 
 * Base interface for all browser automation tools.
 * Each tool represents a specific browser action that can be executed
 * through WebSocket commands to connected clients.
 * 
 * @param <T> The argument type for this tool
 */
public interface BrowserTool<T> {
    
    /**
     * Get the unique name of this tool
     * 
     * @return Tool name used for identification
     */
    String getName();
    
    /**
     * Get the argument type class for this tool
     * 
     * @return Class type of the arguments this tool accepts
     */
    Class<T> getArgumentType();
    
    /**
     * Execute the browser tool with given arguments
     * 
     * @param args Arguments needed to execute this tool
     * @return Result of the tool execution
     */
    ControlDto.ToolResult execute(T args);
    
    /**
     * Get a description of what this tool does
     * 
     * @return Human-readable description of the tool's functionality
     */
    default String getDescription() {
        return "Browser automation tool: " + getName();
    }
    
    /**
     * Check if this tool is currently available for execution
     * 
     * @return true if the tool can be executed, false otherwise
     */
    default boolean isAvailable() {
        return true;
    }
}