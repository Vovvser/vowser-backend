package com.vowser.backend.infrastructure.control.tool;

import com.vowser.backend.api.dto.ControlDto;
import com.vowser.backend.application.service.ControlService;
import com.vowser.backend.common.constants.SpeechConstants;
import com.vowser.backend.common.constants.ToolConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Go Back Tool Implementation
 * 
 * Handles browser back navigation operations.
 * Sends go back commands to connected clients via WebSocket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoBackTool implements BrowserTool<ControlDto.GoBackArgs> {

    private final ControlService controlService;

    @Override
    public String getName() {
        return ToolConstants.ToolNames.GO_BACK;
    }

    @Override
    public Class<ControlDto.GoBackArgs> getArgumentType() {
        return ControlDto.GoBackArgs.class;
    }

    @Override
    public String getDescription() {
        return "Navigates back to the previous page in browser history";
    }

    @Override
    public ControlDto.ToolResult execute(ControlDto.GoBackArgs args) {
        try {
            log.info("뒤로가기 도구 실행");

            Map<String, Object> command = Map.of("action", ToolConstants.ToolActions.GO_BACK);
            controlService.sendCommandToClient(command);

            String successMessage = SpeechConstants.Messages.SUCCESS_GO_BACK_COMMAND_SENT;
            log.info("뒤로가기 명령 전송 완료");

            return new ControlDto.ToolResult(
                List.of(new ControlDto.TextContent(successMessage)), 
                false
            );

        } catch (Exception e) {
            log.error("뒤로가기 도구 실행 중 오류 발생", e);
            
            String errorMessage = String.format("Failed to execute go back command: %s", e.getMessage());
            
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