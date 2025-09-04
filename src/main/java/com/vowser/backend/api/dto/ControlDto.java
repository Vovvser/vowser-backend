package com.vowser.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.vowser.backend.common.constants.ApiConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 제어 관련 데이터 전송 객체
 *
 * 브라우저 제어 작업, 툴 실행,
 * 백엔드와 클라이언트 간 WebSocket 통신을 위한 DTO
 */
public class ControlDto {

    /**
     * 브라우저 툴 실행 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallToolRequest {
        private String toolName;
        private Map<String, Object> args;
    }

    /**
     * 툴 실행 결과
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private List<Content> content;
        private boolean isError = false;
    }

    /**
     * 다양한 콘텐츠 타입의 기본 클래스
     * Jackson 다형성 타입 처리를 사용하여 직렬화를 지원
     */
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TextContent.class, name = ApiConstants.ContentTypes.TEXT),
        @JsonSubTypes.Type(value = ImageContent.class, name = ApiConstants.ContentTypes.IMAGE)
    })
    public abstract static class Content {
        public abstract String getType();
    }

    /**
     * 툴 응답에 사용되는 텍스트 콘텐츠
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextContent extends Content {
        private String type = ApiConstants.ContentTypes.TEXT;
        private String text;

        public TextContent(String text) {
            this.text = text;
        }

        @Override
        public String getType() {
            return type;
        }
    }

    /**
     * 툴 응답에 사용되는 이미지 콘텐츠
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageContent extends Content {
        private String type = ApiConstants.ContentTypes.IMAGE;
        private String data;
        private String mimeType;

        @Override
        public String getType() {
            return type;
        }
    }

    /**
     * 클릭 툴 인자
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClickArgs {
        private String elementId;
    }

    /**
     * 뒤로가기 툴 인자
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoBackArgs {
        private String placeholder;
    }
}