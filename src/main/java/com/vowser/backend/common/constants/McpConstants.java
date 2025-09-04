package com.vowser.backend.common.constants;

public final class McpConstants {
    
    private McpConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final class MessageTypes {
        public static final String SEARCH_PATH = "search_path";
        public static final String SEARCH_PATH_RESULT = "search_path_result";
    }
    
    public static final class Status {
        public static final String SUCCESS = "success";
    }
    
    public static final class ExcludedDomains {
        public static final String EXAMPLE_COM = "example.com";
    }
}