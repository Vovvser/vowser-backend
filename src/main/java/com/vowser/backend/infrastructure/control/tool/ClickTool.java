package com.vowser.backend.infrastructure.control.tool;

import com.vowser.backend.api.dto.ControlDto;
import com.vowser.backend.application.service.ControlService;
import com.vowser.backend.common.constants.ErrorMessages;
import com.vowser.backend.common.constants.ToolConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Click Tool Implementation
 * 
 * Handles element clicking operations on web pages.
 * Sends click commands to connected clients via WebSocket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickTool implements BrowserTool<ControlDto.ClickArgs> {

    private final ControlService controlService;

    @Override
    public String getName() {
        return ToolConstants.ToolNames.CLICK_ELEMENT;
    }

    @Override
    public Class<ControlDto.ClickArgs> getArgumentType() {
        return ControlDto.ClickArgs.class;
    }

    @Override
    public String getDescription() {
        return "Clicks on a specified element using CSS selector or element ID";
    }

    @Override
    public ControlDto.ToolResult execute(ControlDto.ClickArgs args) {
        try {
            log.info("클릭 도구 실행: elementId=[{}]", args.getElementId());
            
            if (args.getElementId() == null || args.getElementId().trim().isEmpty()) {
                log.error("클릭 도구 실행 실패: elementId가 비어있음");
                return new ControlDto.ToolResult(
                    List.of(new ControlDto.TextContent(ErrorMessages.Browser.EMPTY_ELEMENT_ID)), 
                    true
                );
            }

            Map<String, Object> command = Map.of(
                "action", ToolConstants.ToolActions.CLICK,
                "selector", args.getElementId()
            );

            controlService.sendCommandToClient(command);

            String successMessage = String.format("Successfully clicked element: '%s'", args.getElementId());
            log.info("클릭 명령 전송 완료: elementId=[{}]", args.getElementId());

            return new ControlDto.ToolResult(
                List.of(new ControlDto.TextContent(successMessage)), 
                false
            );

        } catch (Exception e) {
            log.error("클릭 도구 실행 중 오류 발생: elementId=[{}]", args.getElementId(), e);
            
            String errorMessage = String.format("Failed to click element '%s': %s", 
                    args.getElementId(), e.getMessage());
            
            return new ControlDto.ToolResult(
                List.of(new ControlDto.TextContent(errorMessage)), 
                true
            );
        }
    }

    @Override
    public boolean isAvailable() {
        return controlService.getActiveSessionCount() > 0;
    }
}