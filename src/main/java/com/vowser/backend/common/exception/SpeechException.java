package com.vowser.backend.common.exception;

public class SpeechException extends BaseException {
    public SpeechException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SpeechException(
            ErrorCode errorCode,
            String detail
    ) {
        super(errorCode, detail);
    }
}
