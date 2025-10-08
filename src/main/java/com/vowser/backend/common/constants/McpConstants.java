package com.vowser.backend.common.constants;

public final class McpConstants {
    
    private McpConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static final class MessageTypes {
        @Deprecated
        public static final String SEARCH_PATH = "search_path";
        @Deprecated
        public static final String SAVE_PATH = "save_path";
        @Deprecated
        public static final String CREATE_INDEXES = "create_indexes";
        public static final String SEARCH_NEW_PATH = "search_new_path";
        public static final String SAVE_NEW_PATH = "save_new_path";
        public static final String CREATE_NEW_INDEXES = "create_new_indexes";
        public static final String CHECK_GRAPH = "check_graph";
        public static final String VISUALIZE_PATHS = "visualize_paths";
        public static final String FIND_POPULAR_PATHS = "find_popular_paths";
        public static final String CLEANUP_PATHS = "cleanup_paths";
        public static final String SAVE_CONTRIBUTION_PATH = "save_contribution_path";

        @Deprecated
        public static final String SEARCH_PATH_RESULT = "search_path_result";
        @Deprecated
        public static final String SAVE_PATH_RESULT = "save_path_result";
    }
    
    public static final class Status {
        public static final String SUCCESS = "success";
    }
}