package com.vowser.backend.common.exception;

import lombok.Getter;

/**
 * 모든 도메인 예외의 기본 클래스
 * 애플리케이션 전체에서 발생하는 모든 커스텀 예외는 이 클래스를 상속받아야 함
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    /**
     * ErrorCode만으로 예외 생성
     */
    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * ErrorCode와 상세 메시지로 예외 생성
     */
    protected BaseException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " - " + detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     */
    protected BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = cause.getMessage();
    }

    /**
     * ErrorCode, 상세 메시지, 원인 예외로 예외 생성
     */
    protected BaseException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + " - " + detail, cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * 실제 응답에 사용될 메시지 반환
     */
    public String getDisplayMessage() {
        if (detail != null && !detail.isEmpty()) {
            return errorCode.getMessage() + " - " + detail;
        }
        return errorCode.getMessage();
    }
}