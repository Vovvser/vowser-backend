package com.vowser.backend.common.exception;

/**
 * 비즈니스 로직 처리 중 발생하는 예외
 */
public class BusinessException extends BaseException {

    /**
     * ErrorCode만으로 비즈니스 예외 생성
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 상세 메시지로 비즈니스 예외 생성
     */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    /**
     * ErrorCode와 원인 예외로 비즈니스 예외 생성
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * ErrorCode, 상세 메시지, 원인 예외로 비즈니스 예외 생성
     */
    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 자주 사용되는 정적 팩토리 메서드들
     */
    public static BusinessException entityNotFound(String entityName) {
        return new BusinessException(ErrorCode.ENTITY_NOT_FOUND, entityName + "을(를) 찾을 수 없습니다");
    }

    public static BusinessException invalidInput(String fieldName) {
        return new BusinessException(ErrorCode.INVALID_INPUT_VALUE, fieldName + " 값이 유효하지 않습니다");
    }

    public static BusinessException duplicateValue(String fieldName, String value) {
        return new BusinessException(ErrorCode.DATA_INTEGRITY_VIOLATION, 
            String.format("%s '%s'은(는) 이미 사용중입니다", fieldName, value));
    }
}