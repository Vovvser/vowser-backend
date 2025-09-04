package com.vowser.backend.api.controller;

import com.vowser.backend.api.doc.BrowserApiDocument;
import com.vowser.backend.api.dto.NavigationDto;
import com.vowser.backend.application.service.ControlService;
import com.vowser.backend.common.constants.ApiConstants;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 브라우저 제어 컨트롤러
 *
 * 브라우저 자동화 및 제어를 위한 REST API를 제공합니다.
 * 명령은 WebSocket 연결을 통해 클라이언트로 전송됩니다.
 */
@Slf4j
@Tag(name = "Browser Control", description = "WebSocket을 통한 실시간 브라우저 제어 API")
@RestController
@RequestMapping(ApiConstants.API_PATH_BROWSER_CONTROL)
@RequiredArgsConstructor
public class BrowserController {

    private final ControlService controlService;

    @BrowserApiDocument.Navigate
    @GetMapping("/navigate")
    public Map<String, String> navigate(
            @Parameter(
                description = "이동할 URL (예: https://google.com)",
                required = true,
                example = "https://google.com"
            )
            @RequestParam String url) {
        
        log.info("브라우저 네비게이션 요청: URL=[{}]", url);
        
        Map<String, Object> commandData = Map.of(
            "type", ApiConstants.BrowserCommands.NAVIGATE,
            "url", url
        );
        
        Map<String, Object> command = Map.of(
            "type", ApiConstants.BrowserCommands.BROWSER_COMMAND_TYPE,
            "data", commandData
        );
        
        controlService.sendCommandToClient(command);
        
        String message = String.format("Navigate command sent to client with URL: %s", url);
        log.info("네비게이션 명령 전송 완료: {}", message);
        
        return Map.of(ApiConstants.RESPONSE_KEY_MESSAGE, message);
    }

   @BrowserApiDocument.GoBack
    @GetMapping("/go-back")
    public Map<String, String> goBack() {
        log.info("브라우저 뒤로가기 요청");
        
        Map<String, Object> commandData = Map.of("type", ApiConstants.BrowserCommands.GO_BACK);
        Map<String, Object> command = Map.of(
            "type", ApiConstants.BrowserCommands.BROWSER_COMMAND_TYPE,
            "data", commandData
        );
        
        controlService.sendCommandToClient(command);
        
        String message = "GoBack command sent to client.";
        log.info("뒤로가기 명령 전송 완료");
        
        return Map.of(ApiConstants.RESPONSE_KEY_MESSAGE, message);
    }

    @BrowserApiDocument.GoForward
    @GetMapping("/go-forward")
    public Map<String, String> goForward() {
        log.info("브라우저 앞으로가기 요청");
        
        Map<String, Object> commandData = Map.of("type", ApiConstants.BrowserCommands.GO_FORWARD);
        Map<String, Object> command = Map.of(
            "type", ApiConstants.BrowserCommands.BROWSER_COMMAND_TYPE,
            "data", commandData
        );
        
        controlService.sendCommandToClient(command);
        
        String message = "GoForward command sent to client.";
        log.info("앞으로가기 명령 전송 완료");
        
        return Map.of(ApiConstants.RESPONSE_KEY_MESSAGE, message);
    }

    @BrowserApiDocument.SendNavigationPath
    @PostMapping("/send-navigation-path")
    public Map<String, Object> sendNavigationPath(
            @RequestBody NavigationDto.AllPathsResponse allPaths) {
        
        log.info("복합 네비게이션 경로 전송 요청: query=[{}], pathCount=[{}]", 
                allPaths.getQuery(), allPaths.getPaths().size());
        
        Map<String, Object> command = Map.of(
            "type", ApiConstants.BrowserCommands.ALL_NAVIGATION_PATHS,
            "data", allPaths
        );
        
        controlService.sendCommandToClient(command);
        
        String message = String.format("All navigation paths for query '%s' sent.", allPaths.getQuery());
        log.info("복합 네비게이션 경로 전송 완료: query=[{}], pathCount=[{}]", 
                allPaths.getQuery(), allPaths.getPaths().size());
        
        return Map.of(
            ApiConstants.RESPONSE_KEY_MESSAGE, message,
            ApiConstants.RESPONSE_KEY_PATH_COUNT, allPaths.getPaths().size()
        );
    }
}