package com.vowser.backend.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vowser.backend.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * API 응답 공통 포맷
 * 성공/실패 여부와 관계없이 일관된 응답 구조를 제공
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorInfo error;
    private LocalDateTime timestamp;

    @Builder(access = AccessLevel.PRIVATE)
    private ApiResponse(boolean success, T data, ErrorInfo error) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    /**
     * 실패 응답 생성 (ErrorCode 사용)
     */
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ErrorInfo.of(errorCode))
                .build();
    }

    /**
     * 실패 응답 생성 (ErrorCode와 상세 메시지)
     */
    public static ApiResponse<Void> error(ErrorCode errorCode, String detail) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ErrorInfo.of(errorCode, detail))
                .build();
    }

    /**
     * 실패 응답 생성 (HttpStatus와 메시지)
     */
    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ErrorInfo.of(status, message))
                .build();
    }
    
    /**
     * 실패 응답 생성 (HttpStatus와 메시지) - 제네릭 타입 지원
     */
    public static <T> ApiResponse<T> errorWithType(HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.of(status, message))
                .build();
    }

    /**
     * 에러 정보 내부 클래스
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        private String code;
        private String message;
        private String detail;
        private int status;

        @Builder(access = AccessLevel.PRIVATE)
        private ErrorInfo(String code, String message, String detail, int status) {
            this.code = code;
            this.message = message;
            this.detail = detail;
            this.status = status;
        }

        public static ErrorInfo of(ErrorCode errorCode) {
            return ErrorInfo.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .status(errorCode.getStatus().value())
                    .build();
        }

        public static ErrorInfo of(ErrorCode errorCode, String detail) {
            return ErrorInfo.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .detail(detail)
                    .status(errorCode.getStatus().value())
                    .build();
        }

        public static ErrorInfo of(HttpStatus status, String message) {
            return ErrorInfo.builder()
                    .code(status.name())
                    .message(message)
                    .status(status.value())
                    .build();
        }
    }
}