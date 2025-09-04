package com.vowser.backend.common.constants;

public final class ApiConstants {
    
    private ApiConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final String API_PATH_BROWSER_CONTROL = "/browser-control";
    public static final String API_PATH_SPEECH = "/api/v1/speech";
    
    public static final String RESPONSE_KEY_MESSAGE = "message";
    public static final String RESPONSE_KEY_CONNECTED = "connected";
    public static final String RESPONSE_KEY_PATH_COUNT = "pathCount";
    
    public static final class BrowserCommands {
        public static final String BROWSER_COMMAND_TYPE = "browser_command";
        public static final String NAVIGATE = "navigate";
        public static final String GO_BACK = "go_back";
        public static final String GO_FORWARD = "go_forward";
        public static final String ALL_NAVIGATION_PATHS = "all_navigation_paths";
    }
    
    public static final class ContentTypes {
        public static final String TEXT = "text";
        public static final String IMAGE = "image";
    }
}