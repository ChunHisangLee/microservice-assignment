package com.jack.common.constants;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ErrorCode {
    MAIL_ALREADY_EXISTS(20001, "Email already registered by another user.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(20002, "User not found.", HttpStatus.NOT_FOUND),
    WALLET_ALREADY_EXISTS(20003, "Wallet already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_ACTIVE(20004, "User not active", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND(20005, "Wallet not found", HttpStatus.NOT_FOUND),
    WALLET_NOT_ENOUGH_BALANCE(20006, "Wallet not enough balance", HttpStatus.BAD_REQUEST),
    WALLET_NOT_ACTIVE(20007, "Wallet not active", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_OR_PASSWORD(20008, "Invalid email or password.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_REQUEST(20009, "Unauthorized request. Please try again.", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus status;

    private static final Map<Integer, ErrorCode> CODE_MAP = Stream.of(values())
            .collect(Collectors.toMap(ErrorCode::getCode, e -> e));

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ErrorCode fromCode(int code) {
        return CODE_MAP.get(code);
    }
}
