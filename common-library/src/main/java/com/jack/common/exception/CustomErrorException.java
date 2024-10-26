package com.jack.common.exception;

import com.jack.common.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomErrorException extends RuntimeException {
    private final int statusCode;
    private final String status;
    private final String path;

    // Constructor to initialize fields directly from ErrorCode
    public CustomErrorException(ErrorCode errorCode, String path) {
        super(errorCode.getMessage());
        this.statusCode = errorCode.getHttpStatus().value();
        this.status = errorCode.getHttpStatus().getReasonPhrase();
        this.path = path;
    }
}
