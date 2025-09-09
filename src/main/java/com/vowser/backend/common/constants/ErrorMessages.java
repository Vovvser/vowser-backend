package com.vowser.backend.common.constants;

public final class ErrorMessages {
    
    private ErrorMessages() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final class Tool {
        public static final String TOOL_NOT_FOUND = "도구를 찾을 수 없습니다: ";
        public static final String TOOL_NOT_AVAILABLE = "사용할 수 없는 도구입니다: ";
        public static final String TOOL_EXECUTION_FAILED = "도구 실행에 실패했습니다: ";
        public static final String ARGUMENT_CONVERSION_FAILED = "인수 변환에 실패했습니다: ";
    }
    
    public static final class WebSocket {
        public static final String INVALID_JSON_FORMAT = "잘못된 JSON 형식입니다: ";
        public static final String MESSAGE_PROCESSING_FAILED = "메시지 처리에 실패했습니다: ";
        public static final String WELCOME_MESSAGE_PREFIX = "Vowser 백엔드 제어 서비스에 연결되었습니다. 사용 가능한 도구: ";
    }
    
    public static final class Browser {
        public static final String EMPTY_ELEMENT_ID = "오류: 요소 ID가 비어있습니다";
        public static final String EMPTY_URL = "오류: URL이 비어있습니다";
        public static final String INVALID_URL_FORMAT = "오류: 잘못된 URL 형식입니다 - ";
    }
    
    public static final class MCP {
        public static final String APPLICATION_SHUTDOWN = "애플리케이션 종료";
    }
}