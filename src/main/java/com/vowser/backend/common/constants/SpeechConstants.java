package com.vowser.backend.common.constants;

public final class SpeechConstants {
    
    private SpeechConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final class Language {
        public static final String KOREAN = "ko-KR";
    }
    
    public static final class Model {
        public static final String LONG = "long";
    }
    
    public static final class Messages {
        public static final String SUCCESS_GO_BACK_COMMAND_SENT = "브라우저 네비게이션: 클라이언트로 뒤로가기 명령 전송됨";
    }
}