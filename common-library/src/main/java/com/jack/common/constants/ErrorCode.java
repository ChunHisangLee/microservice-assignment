package com.jack.common.constants;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // --- User-related Errors ---
    MAIL_ALREADY_EXISTS(20001, "Email already registered by another user.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(20002, "User not found.", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(20004, "User not active", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_OR_PASSWORD(20008, "Invalid email or password.", HttpStatus.BAD_REQUEST),

    // --- Wallet-related Errors ---
    WALLET_ALREADY_EXISTS(20003, "Wallet already exists", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND(20005, "Wallet not found", HttpStatus.NOT_FOUND),
    WALLET_NOT_ENOUGH_BALANCE(20006, "Wallet does not have enough balance", HttpStatus.BAD_REQUEST),
    WALLET_NOT_ACTIVE(20007, "Wallet not active", HttpStatus.BAD_REQUEST),
    WALLET_SERVICE_ERROR(20012, "Failed to retrieve wallet balance from wallet-service.", HttpStatus.SERVICE_UNAVAILABLE),

    // --- Authentication and Authorization Errors ---
    UNAUTHORIZED_REQUEST(20009, "Unauthorized request. Please try again.", HttpStatus.BAD_REQUEST),
    NO_VALID_TOKEN(20011, "No valid token found.", HttpStatus.UNAUTHORIZED),

    // --- Service Errors ---
    LOGOUT_SERVICE_ERROR(20010, "Auth service error during logout.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
