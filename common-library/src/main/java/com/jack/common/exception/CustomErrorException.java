package com.jack.common.exception;

import org.springframework.http.HttpStatus;

public class CustomErrorException extends RuntimeException {
    private final int statusCode;
    private final String status;
    private final String message;
    private final String path;

    // Accept HttpStatus instead of separate status code and status
    public CustomErrorException(HttpStatus httpStatus, String message, String path) {
        super(message);
        this.statusCode = httpStatus.value();
        this.status = httpStatus.getReasonPhrase();
        this.message = message;
        this.path = path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
