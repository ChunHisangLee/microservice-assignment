package com.jack.common.constants;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum ErrorPath {
    // --- User-related APIs ---
    GET_USER_API(10001, "GET /api/users/{userId}", HttpStatus.BAD_REQUEST),
    POST_USER_API(10002, "POST /api/users", HttpStatus.NOT_FOUND),
    PUT_UPDATE_USER_API(10003, "PUT /api/users/{id}", HttpStatus.BAD_REQUEST),
    DELETE_USER_API(10004, "DELETE /api/users/{userId}", HttpStatus.BAD_REQUEST),
    POST_LOGIN_API(10005, "POST /api/users/login", HttpStatus.NOT_FOUND),
    GET_LOGOUT_API(10006, "GET /api/users/logout", HttpStatus.UNAUTHORIZED),
    POST_REGISTER_API(10007, "POST /api/users/register", HttpStatus.BAD_REQUEST),
    POST_VERIFY_PASSWORD_API(10008, "POST /api/users/verify-password", HttpStatus.BAD_REQUEST),
    POST_LOGOUT_API(10009, "POST /api/users/logout", HttpStatus.INTERNAL_SERVER_ERROR),
    GET_WALLET_BALANCE_API(10010, "GET /api/wallet/{userId}/balance", HttpStatus.BAD_REQUEST),
    GET_USER_BALANCE_API(10011, "GET /api/users/{userId}/balance", HttpStatus.BAD_REQUEST),

    // --- Transaction-related APIs ---
    POST_TRANSACTION_API(10011, "POST /api/transactions", HttpStatus.BAD_REQUEST),
    GET_TRANSACTION_API(10012, "GET /api/transactions/{transactionId}", HttpStatus.NOT_FOUND),
    PUT_TRANSACTION_API(10013, "PUT /api/transactions/{transactionId}", HttpStatus.BAD_REQUEST),
    DELETE_TRANSACTION_API(10014, "DELETE /api/transactions/{transactionId}", HttpStatus.BAD_REQUEST),

    // --- Payment-related APIs ---
    POST_PAYMENT_API(10015, "POST /api/payments", HttpStatus.BAD_REQUEST),
    GET_PAYMENT_API(10016, "GET /api/payments/{paymentId}", HttpStatus.NOT_FOUND),
    PUT_PAYMENT_API(10017, "PUT /api/payments/{paymentId}", HttpStatus.BAD_REQUEST),
    DELETE_PAYMENT_API(10018, "DELETE /api/payments/{paymentId}", HttpStatus.BAD_REQUEST),

    // --- Inventory-related APIs ---
    GET_INVENTORY_API(10019, "GET /api/inventory", HttpStatus.BAD_REQUEST),
    POST_INVENTORY_API(10020, "POST /api/inventory", HttpStatus.NOT_FOUND),
    PUT_INVENTORY_API(10021, "PUT /api/inventory/{itemId}", HttpStatus.BAD_REQUEST),
    DELETE_INVENTORY_API(10022, "DELETE /api/inventory/{itemId}", HttpStatus.BAD_REQUEST),

    // --- Reporting-related APIs ---
    GET_REPORT_API(10023, "GET /api/reports/{reportId}", HttpStatus.NOT_FOUND),
    POST_REPORT_API(10024, "POST /api/reports", HttpStatus.BAD_REQUEST),
    GENERATE_REPORT_API(10025, "POST /api/reports/generate", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Notification-related APIs ---
    SEND_NOTIFICATION_API(10026, "POST /api/notifications/send", HttpStatus.INTERNAL_SERVER_ERROR),
    GET_NOTIFICATION_API(10027, "GET /api/notifications/{notificationId}", HttpStatus.NOT_FOUND),
    DELETE_NOTIFICATION_API(10028, "DELETE /api/notifications/{notificationId}", HttpStatus.BAD_REQUEST),

    // --- File-related APIs ---
    UPLOAD_FILE_API(10029, "POST /api/files/upload", HttpStatus.INTERNAL_SERVER_ERROR),
    DOWNLOAD_FILE_API(10030, "GET /api/files/{fileId}/download", HttpStatus.NOT_FOUND),
    DELETE_FILE_API(10031, "DELETE /api/files/{fileId}", HttpStatus.BAD_REQUEST),

    // --- Authentication-related APIs ---
    REFRESH_TOKEN_API(10032, "POST /api/auth/refresh-token", HttpStatus.UNAUTHORIZED),
    FORGOT_PASSWORD_API(10033, "POST /api/auth/forgot-password", HttpStatus.BAD_REQUEST),
    RESET_PASSWORD_API(10034, "POST /api/auth/reset-password", HttpStatus.BAD_REQUEST),

    // --- Outbox-related APIs ---
    OUTBOX_EVENT_API(10032, "POST /api/user/register", HttpStatus.BAD_REQUEST),

    // --- System-related APIs ---
    HEALTH_CHECK_API(10035, "GET /api/system/health", HttpStatus.SERVICE_UNAVAILABLE),
    METRICS_API(10036, "GET /api/system/metrics", HttpStatus.INTERNAL_SERVER_ERROR);


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

    public static ErrorPath fromCode(int code) {
        return CODE_MAP.get(code);
    }
}
