package com.jack.common.constants;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ErrorPath {
    GET_USER_API(10001, "GET /api/users/", HttpStatus.BAD_REQUEST),
    POST_USER_API(10002, "POST /api/users", HttpStatus.NOT_FOUND),
    PUT_USER_API(10003, "PUT /api/users/", HttpStatus.BAD_REQUEST),
    DELETE_USER_API(10004, "DELETE /api/users", HttpStatus.BAD_REQUEST),
    POST_LOGIN_API(10005, "POST /api/users/login", HttpStatus.NOT_FOUND),
    GET_LOGOUT_API(10006, "GET /api/users/logout", HttpStatus.UNAUTHORIZED),
    POST_REGISTER_API(10007, "POST /api/users/register", HttpStatus.BAD_REQUEST),
    POST_VERIFY_PASSWORD_API(10008, "POST /api/users/verify-password", HttpStatus.BAD_REQUEST),
    POST_LOGOUT_API(10009, "POST /api/users/logout", HttpStatus.INTERNAL_SERVER_ERROR),
    GET_WALLET_BALANCE_API(10010, "GET /api/users//{userId}/balance", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String path;
    private final HttpStatus status;

    private static final Map<Integer, ErrorPath> CODE_MAP = Stream.of(values())
            .collect(Collectors.toMap(ErrorPath::getCode, e -> e));

    ErrorPath(int code, String path, HttpStatus status) {
        this.code = code;
        this.path = path;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getPath() {
        return path;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ErrorPath fromCode(int code) {
        return CODE_MAP.get(code);
    }
}
