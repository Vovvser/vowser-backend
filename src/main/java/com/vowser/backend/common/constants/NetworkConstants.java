package com.vowser.backend.common.constants;

public final class NetworkConstants {
    
    private NetworkConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final class WebSocket {
        public static final int WRITE_TIMEOUT_SECONDS = 10;
        public static final int CONNECT_TIMEOUT_SECONDS = 30;
        public static final int NORMAL_CLOSURE_CODE = 1000;
    }
    
    public static final class DataSize {
        public static final int BYTES_PER_KB = 1024;
    }
    
    public static final class MCP {
        public static final int SEARCH_PATH_LIMIT = 3;
    }
}