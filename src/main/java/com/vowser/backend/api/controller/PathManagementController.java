package com.vowser.backend.api.controller;

import com.vowser.backend.api.dto.mcp.*;
import com.vowser.backend.infrastructure.mcp.McpWebSocketClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * MCP 서버 경로 관리 API 컨트롤러
 *
 * db-refactor 브랜치의 새로운 API 구조를 사용
 */
@Slf4j
@Tag(name = "Path Management", description = "경로 저장/검색/관리 API (db-refactor)")
@RestController
@RequestMapping("/api/v1/paths")
@RequiredArgsConstructor
public class PathManagementController {

    private final McpWebSocketClient mcpClient;

    /**
     * 경로 저장 (새 구조)
     *
     * POST /api/v1/paths
     * Body: PathSubmission
     */
    @Operation(summary = "경로 저장", description = "새로운 네비게이션 경로를 저장")
    @PostMapping
    public CompletableFuture<SavePathResponse> savePath(@RequestBody PathSubmission pathSubmission) {
        log.info("Received path save request: taskIntent=[{}], domain=[{}]",
                pathSubmission.getTaskIntent(), pathSubmission.getDomain());
        return mcpClient.savePath(pathSubmission);
    }

    /**
     * 자연어 경로 검색 (새 구조)
     *
     * GET /api/v1/paths/search?query=유튜브 음악&limit=3&domain=youtube.com
     */
    @Operation(summary = "경로 검색", description = "자연어 쿼리로 경로를 검색")
    @GetMapping("/search")
    public CompletableFuture<SearchPathResponse> searchPath(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(required = false) String domain) {

        log.info("Received search request: query=[{}], domain=[{}]", query, domain);
        return mcpClient.searchPath(query, limit, domain);
    }

    /**
     * 그래프 통계
     *
     * GET /api/v1/paths/graph/stats
     */
    @Operation(summary = "그래프 통계", description = "Neo4j 그래프 데이터베이스 통계 조회")
    @GetMapping("/graph/stats")
    public CompletableFuture<GraphStatsResponse> getGraphStats() {
        return mcpClient.checkGraph();
    }

    /**
     * 도메인별 경로 시각화
     *
     * GET /api/v1/paths/graph/visualize/{domain}
     */
    @Operation(summary = "경로 시각화", description = "특정 도메인의 경로 그래프를 시각화용 데이터로 반환")
    @GetMapping("/graph/visualize/{domain}")
    public CompletableFuture<VisualizePathsResponse> visualizePaths(@PathVariable String domain) {
        return mcpClient.visualizePaths(domain);
    }

    /**
     * 인기 경로 조회
     *
     * GET /api/v1/paths/popular?domain=youtube.com&limit=10
     */
    @Operation(summary = "인기 경로", description = "특정 도메인에서 가장 많이 사용된 경로 조회 (weight 기준)")
    @GetMapping("/popular")
    public CompletableFuture<PopularPathsResponse> getPopularPaths(
            @RequestParam String domain,
            @RequestParam(defaultValue = "10") int limit) {

        return mcpClient.findPopularPaths(domain, limit);
    }

    /**
     * 인덱스 생성 (관리자용)
     *
     * POST /api/v1/paths/admin/indexes
     */
    @Operation(summary = "벡터 인덱스 생성", description = "taskIntent 임베딩 벡터 인덱스 생성 (관리자용)")
    @PostMapping("/admin/indexes")
    public CompletableFuture<IndexResponse> createIndexes() {
        log.info("Admin: Creating indexes");
        return mcpClient.createIndexes();
    }

    /**
     * 경로 정리 (스케줄러용)
     *
     * POST /api/v1/paths/admin/cleanup
     */
    @Operation(summary = "경로 정리", description = "오래된 경로 및 관계 정리 (스케줄러용)")
    @PostMapping("/admin/cleanup")
    public CompletableFuture<CleanupResponse> cleanupPaths() {
        log.info("Admin: Cleaning up paths");
        return mcpClient.cleanupPaths();
    }
}