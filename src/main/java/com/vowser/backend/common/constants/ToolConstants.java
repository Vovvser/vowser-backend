package com.vowser.backend.common.constants;

public final class ToolConstants {
    
    private ToolConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final class ToolNames {
        public static final String CLICK_ELEMENT = "clickElement";
        public static final String NAVIGATE = "navigate";
        public static final String GO_BACK = "goBack";
    }
    
    public static final class ToolActions {
        public static final String CLICK = "click";
        public static final String NAVIGATE = "navigate";
        public static final String GO_BACK = "goBack";
    }
    
    public static final class ClientActions {
        public static final String NAVIGATE = "navigate";
        public static final String CLICK = "click";
        public static final String TYPE = "type";
        
        public static final String NAVIGATE_KR = "이동";
        public static final String ACCESS_KR = "접속";
        public static final String CLICK_KR = "클릭";
        public static final String TYPE_KR = "입력";
    }
}