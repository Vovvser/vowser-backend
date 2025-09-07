package com.vowser.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전체에서 사용되는 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "엔티티를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 타입의 값입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근이 거부되었습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C007", "리소스를 찾을 수 없습니다"),

    // Authentication & Authorization
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A002", "인증에 실패했습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰을 찾을 수 없습니다"),
    NAVER_OAUTH2_FAILED(HttpStatus.UNAUTHORIZED, "A006", "네이버 로그인에 실패했습니다"),
    
    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용중인 이메일입니다"),
    MEMBER_PROFILE_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "M003", "회원 프로필 업데이트에 실패했습니다"),
    
    // Business Logic
    INVALID_BUSINESS_LOGIC(HttpStatus.BAD_REQUEST, "B001", "비즈니스 로직 오류가 발생했습니다"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "B002", "데이터 무결성 위반이 발생했습니다"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "B003", "동시 수정 충돌이 발생했습니다"),
    
    // External Service
    EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E001", "외부 서비스 오류가 발생했습니다"),
    MCP_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E002", "MCP 서버 오류가 발생했습니다"),
    REDIS_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E003", "Redis 연결 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    /**
     * 에러코드로 ErrorCode 찾기
     */
    public static ErrorCode findByCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}