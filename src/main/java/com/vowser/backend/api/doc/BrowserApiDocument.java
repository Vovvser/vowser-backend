package com.vowser.backend.api.doc;

import com.vowser.backend.api.dto.NavigationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.http.MediaType;

@Tag(name = "Browser Control", description = "WebSocket을 통한 실시간 브라우저 제어 API")
public @interface BrowserApiDocument {

    @Operation(
            summary = "브라우저 페이지 이동",
            description = "WebSocket으로 연결된 클라이언트의 브라우저를 지정된 URL로 이동시킵니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "네비게이션 명령이 성공적으로 전송됨",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Navigate command sent to client with URL: https://google.com\"}"
                            )
                    )
            )
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Navigate {}


    @Operation(
            summary = "브라우저 뒤로가기",
            description = "WebSocket으로 연결된 클라이언트의 브라우저에서 이전 페이지로 이동합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "뒤로가기 명령이 성공적으로 전송됨",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"message\": \"GoBack command sent to client.\"}")
                    )
            )
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface GoBack {}


    @Operation(
            summary = "브라우저 앞으로가기",
            description = "WebSocket으로 연결된 클라이언트의 브라우저에서 다음 페이지로 이동합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "앞으로가기 명령이 성공적으로 전송됨",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"message\": \"GoForward command sent to client.\"}")
                    )
            )
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface GoForward {}


    @Operation(
            summary = "복합 네비게이션 경로 전송",
            description = "여러 경로가 포함된 AllPathsResponse를 받아 클라이언트에게 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "네비게이션 경로가 성공적으로 전송됨",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NavigationDto.NavigationPathResponse.class),
                            examples = @ExampleObject(
                                    value = "{\n \"message\": \"Navigation path sent to client\",\n \"pathId\": \"test_path_Google Search_query\",\n \"stepCount\": 3\n}"
                            )
                    )
            )
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface SendNavigationPath {}
}
