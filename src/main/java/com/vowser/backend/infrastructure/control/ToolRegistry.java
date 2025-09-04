package com.vowser.backend.infrastructure.control;

import com.vowser.backend.infrastructure.control.tool.BrowserTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용 가능한 모든 브라우저 자동화 도구의 중앙 레지스트리
 * 도구 검색, 등록, 조회를 관리
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, BrowserTool<?>> toolMap;

    /**
     * 모든 사용 가능한 도구를 자동으로 등록하는 생성자
     *
     * @param tools Spring에 의해 자동 주입되는 모든 BrowserTool 구현체 목록
     */
    ToolRegistry(List<BrowserTool<?>> tools) {
        this.toolMap = tools.stream()
                .collect(Collectors.toMap(
                    BrowserTool::getName,
                    tool -> tool,
                    (existing, replacement) -> {
                        log.warn("중복된 도구 이름 발견: {}. 기존 도구를 유지합니다.", existing.getName());
                        return existing;
                    }
                ));
        
        log.info("도구 레지스트리 초기화 완료: 등록된 도구 수=[{}], 도구 목록=[{}]", 
                toolMap.size(), 
                String.join(", ", toolMap.keySet()));
    }

    /**
     * 이름으로 도구를 가져옴
     *
     * @param name 조회할 도구 이름
     * @return BrowserTool 인스턴스 (없으면 null 반환)
     */
    public BrowserTool<?> getTool(String name) {
        BrowserTool<?> tool = toolMap.get(name);
        
        if (tool == null) {
            log.warn("요청된 도구를 찾을 수 없음: toolName=[{}], availableTools=[{}]", 
                    name, String.join(", ", toolMap.keySet()));
        } else {
            log.debug("도구 검색 성공: toolName=[{}], toolClass=[{}]", 
                    name, tool.getClass().getSimpleName());
        }
        
        return tool;
    }

    /**
     * 현재 사용 가능한 도구 목록을 반환
     *
     * @return 사용 가능한 도구 이름 리스트
     */
    public List<String> getAvailableToolNames() {
        return toolMap.values().stream()
                .filter(BrowserTool::isAvailable)
                .map(BrowserTool::getName)
                .collect(Collectors.toList());
    }
}