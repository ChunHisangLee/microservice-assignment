package com.jack.walletservice.exception;

import com.jack.walletservice.response.CustomErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomErrorException.class)
    public ResponseEntity<CustomErrorResponse> handleCustomError(CustomErrorException ex) {
        CustomErrorResponse errorResponse = new CustomErrorResponse(
                ex.getStatusCode(),
                ex.getStatus(),
                ex.getMessage(),
                ex.getPath()
        );

        HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode());

        // Handle invalid status codes safely by defaulting to INTERNAL_SERVER_ERROR if necessary
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(errorResponse, httpStatus);
    }
}
