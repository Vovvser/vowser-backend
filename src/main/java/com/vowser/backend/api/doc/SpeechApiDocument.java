package com.vowser.backend.api.doc;

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

@Tag(name = "Speech Processing", description = "음성 인식 및 처리 API")
public @interface SpeechApiDocument {

    @Operation(
            summary = "음성 파일 인식 및 명령 실행",
            description = """
            업로드된 음성 파일을 Google Cloud Speech-to-Text로 인식하고,
            인식된 텍스트를 MCP 서버로 전송하여 브라우저 자동화를 실행합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음성 인식 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.vowser.backend.api.dto.SpeechDto.SpeechResponse.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "success": true,
                          "transcript": "구글에서 자바 튜토리얼을 검색해줘",
                          "message": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "음성 인식 실패 또는 MCP 서버 연결 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "success": false,
                          "transcript": null,
                          "message": "MCP 서버에 연결되지 않음"
                        }
                        """
                            )
                    )
            )
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TranscribeAndExecute {}

    @Operation(
            summary = "MCP 서버 연결 상태 확인",
            description = "MCP 서버와의 WebSocket 연결 상태를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연결 상태 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "connected", value = "{\"connected\": true}"),
                                    @ExampleObject(name = "disconnected", value = "{\"connected\": false}")
                            }
                    )
            )
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface McpStatus {}
}