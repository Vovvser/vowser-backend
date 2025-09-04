package com.vowser.backend.infrastructure.control.tool;

import com.vowser.backend.api.dto.ControlDto;
import com.vowser.backend.application.service.ControlService;
import com.vowser.backend.common.constants.ErrorMessages;
import com.vowser.backend.common.constants.ToolConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 특정 URL로의 브라우저 네비게이션 작업을 처리
 * WebSocket을 통해 연결된 클라이언트에 네비게이션 명령을 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NavigationTool implements BrowserTool<NavigationTool.NavigationArgs> {

    private final ControlService controlService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationArgs {
        private String url;
    }

    @Override
    public String getName() {
        return ToolConstants.ToolNames.NAVIGATE;
    }

    @Override
    public Class<NavigationArgs> getArgumentType() {
        return NavigationArgs.class;
    }

    @Override
    public String getDescription() {
        return "Navigates browser to a specified URL";
    }

    @Override
    public ControlDto.ToolResult execute(NavigationArgs args) {
        try {
            log.info("네비게이션 도구 실행: url=[{}]", args.getUrl());
            
            if (args.getUrl() == null || args.getUrl().trim().isEmpty()) {
                log.error("네비게이션 도구 실행 실패: URL이 비어있음");
                return new ControlDto.ToolResult(
                    List.of(new ControlDto.TextContent(ErrorMessages.Browser.EMPTY_URL)), 
                    true
                );
            }

            String url = normalizeUrl(args.getUrl());
            if (!isValidUrl(url)) {
                log.error("네비게이션 도구 실행 실패: 잘못된 URL 형식 - {}", url);
                return new ControlDto.ToolResult(
                    List.of(new ControlDto.TextContent(ErrorMessages.Browser.INVALID_URL_FORMAT + url)), 
                    true
                );
            }

            Map<String, Object> command = Map.of(
                "action", ToolConstants.ToolActions.NAVIGATE,
                "url", url
            );

            controlService.sendCommandToClient(command);

            String successMessage = String.format("Successfully navigated to: %s", url);
            log.info("네비게이션 명령 전송 완료: url=[{}]", url);

            return new ControlDto.ToolResult(
                List.of(new ControlDto.TextContent(successMessage)), 
                false
            );

        } catch (Exception e) {
            log.error("네비게이션 도구 실행 중 오류 발생: url=[{}]", args.getUrl(), e);
            
            String errorMessage = String.format("Failed to navigate to '%s': %s", 
                    args.getUrl(), e.getMessage());
            
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

    /**
     * URL에 프로토콜이 없으면 추가하여 정규화
     *
     * @param url 원시 URL 문자열
     * @return 프로토콜이 포함된 정규화된 URL
     */
    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    /**
     * URL 형식을 검증
     *
     * @param url 검증할 URL
     * @return 유효하면 true, 그렇지 않으면 false
     */
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}