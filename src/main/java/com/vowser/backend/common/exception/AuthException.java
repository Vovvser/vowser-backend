package com.vowser.backend.common.exception;

/**
 * 인증/인가 처리 중 발생하는 예외
 */
public class AuthException extends BaseException {

    /**
     * ErrorCode만으로 인증 예외 생성
     */
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 상세 메시지로 인증 예외 생성
     */
    public AuthException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    /**
     * ErrorCode와 원인 예외로 인증 예외 생성
     */
    public AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * ErrorCode, 상세 메시지, 원인 예외로 인증 예외 생성
     */
    public AuthException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 자주 사용되는 정적 팩토리 메서드들
     */
    public static AuthException unauthorized() {
        return new AuthException(ErrorCode.UNAUTHORIZED);
    }

    public static AuthException authenticationFailed(String reason) {
        return new AuthException(ErrorCode.AUTHENTICATION_FAILED, reason);
    }

    public static AuthException tokenExpired() {
        return new AuthException(ErrorCode.TOKEN_EXPIRED);
    }

    public static AuthException invalidToken(String detail) {
        return new AuthException(ErrorCode.INVALID_TOKEN, detail);
    }

    public static AuthException accessDenied(String resource) {
        return new AuthException(ErrorCode.ACCESS_DENIED, "리소스 '" + resource + "'에 대한 접근이 거부되었습니다");
    }

    public static AuthException naverLoginFailed(String reason) {
        return new AuthException(ErrorCode.NAVER_OAUTH2_FAILED, reason);
    }
}