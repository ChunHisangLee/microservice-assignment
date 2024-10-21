package com.jack.common.constants;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // --- User-related Errors ---
    MAIL_ALREADY_EXISTS(20001, "Email already registered by another user.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(20002, "User not found.", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(20004, "User not active.", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_OR_PASSWORD(20008, "Invalid email or password.", HttpStatus.BAD_REQUEST),

    // --- Wallet-related Errors ---
    WALLET_ALREADY_EXISTS(20003, "Wallet already exists.", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND(20005, "Wallet not found.", HttpStatus.NOT_FOUND),
    WALLET_NOT_ENOUGH_BALANCE(20006, "Wallet does not have enough balance.", HttpStatus.BAD_REQUEST),
    WALLET_NOT_ACTIVE(20007, "Wallet not active.", HttpStatus.BAD_REQUEST),
    WALLET_SERVICE_ERROR(20012, "Failed to retrieve wallet balance from wallet-service.", HttpStatus.SERVICE_UNAVAILABLE),

    // --- Authentication and Authorization Errors ---
    UNAUTHORIZED_REQUEST(20009, "Unauthorized request. Please try again.", HttpStatus.BAD_REQUEST),
    NO_VALID_TOKEN(20011, "No valid token found.", HttpStatus.UNAUTHORIZED),

    // --- Service Errors ---
    LOGOUT_SERVICE_ERROR(20010, "Auth service error during logout.", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Transaction-related Errors ---
    TRANSACTION_NOT_FOUND(20013, "Transaction not found.", HttpStatus.NOT_FOUND),
    TRANSACTION_ALREADY_COMPLETED(20014, "Transaction has already been completed.", HttpStatus.BAD_REQUEST),
    TRANSACTION_FAILED(20015, "Transaction failed to process.", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Payment-related Errors ---
    PAYMENT_METHOD_INVALID(20016, "Invalid payment method.", HttpStatus.BAD_REQUEST),
    PAYMENT_PROCESSING_ERROR(20017, "Error processing payment.", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Validation Errors ---
    INVALID_INPUT_DATA(20018, "Invalid input data.", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(20019, "Missing required field.", HttpStatus.BAD_REQUEST),

    // --- File-related Errors ---
    FILE_UPLOAD_FAILED(20020, "File upload failed.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(20021, "File not found.", HttpStatus.NOT_FOUND),
    FILE_TOO_LARGE(20022, "Uploaded file is too large.", HttpStatus.BAD_REQUEST),

    // --- Permission Errors ---
    ACCESS_DENIED(20023, "Access denied.", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS(20024, "Insufficient permissions.", HttpStatus.FORBIDDEN),

    // --- System Errors ---
    DATABASE_CONNECTION_ERROR(20025, "Error connecting to the database.", HttpStatus.INTERNAL_SERVER_ERROR),
    CACHE_SERVICE_UNAVAILABLE(20026, "Cache service is unavailable.", HttpStatus.SERVICE_UNAVAILABLE),
    EXTERNAL_API_FAILURE(20027, "External API call failed.", HttpStatus.BAD_GATEWAY),

    // --- Notification Errors ---
    NOTIFICATION_SEND_FAILED(20028, "Failed to send notification.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_NOTIFICATION_CHANNEL(20029, "Invalid notification channel specified.", HttpStatus.BAD_REQUEST),

    // --- Inventory Errors ---
    ITEM_OUT_OF_STOCK(20030, "Item is out of stock.", HttpStatus.CONFLICT),
    INVENTORY_UPDATE_FAILED(20031, "Failed to update inventory.", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Reporting Errors ---
    REPORT_GENERATION_FAILED(20032, "Failed to generate report.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REPORT_PARAMETERS(20033, "Invalid parameters provided for report generation.", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

}
