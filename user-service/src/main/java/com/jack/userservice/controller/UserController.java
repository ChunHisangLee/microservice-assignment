package com.jack.userservice.controller;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.common.dto.response.UserRegistrationDto;
import com.jack.common.dto.response.UserResponseDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.service.UserService;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthServiceClient authServiceClient;

    public UserController(UserService userService, AuthServiceClient authServiceClient) {
        this.userService = userService;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationDto userRegistrationDTO) {
        UserResponseDto userResponse = userService.register(userRegistrationDTO);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@Valid @RequestBody AuthRequestDto authRequestDto) {
        boolean isPasswordValid = userService.verifyPassword(authRequestDto.getEmail(), authRequestDto.getPassword());
        return ResponseEntity.ok(isPasswordValid);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDto loginRequest) {
        logger.info("User login attempt with email: {}", loginRequest.getEmail());

        if (!userService.verifyPassword(loginRequest.getEmail(), loginRequest.getPassword())) {
            logger.error("Invalid credentials for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus())
                    .body(new CustomErrorException(
                            ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus(),
                            ErrorCode.INVALID_EMAIL_OR_PASSWORD.getMessage(),
                            ErrorPath.POST_LOGIN_API.getPath()
                    ));
        }

        try {
            AuthResponseDto authResponse = authServiceClient.login(loginRequest);
            logger.info("User with email: {} logged in successfully.", loginRequest.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (FeignException e) {
            logger.error("Error during login via auth-service: {}", e.getMessage());
            return ResponseEntity.status(ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus())
                    .body(new CustomErrorException(
                            ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus(),
                            ErrorCode.LOGOUT_SERVICE_ERROR.getMessage(),
                            ErrorPath.POST_LOGIN_API.getPath()
                    ));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        if (token != null && token.startsWith(SecurityConstants.BEARER_PREFIX)) {
            try {
                authServiceClient.logout(token);
                logger.info("Logout request sent to auth-service with token: {}", token);
                return ResponseEntity.ok().build();
            } catch (FeignException e) {
                logger.error("Error during logout via auth-service: {}", e.getMessage());
                return ResponseEntity.status(ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus())
                        .body(new CustomErrorException(
                                ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus(),
                                ErrorCode.LOGOUT_SERVICE_ERROR.getMessage(),
                                ErrorPath.GET_LOGOUT_API.getPath()
                        ));
            }
        } else {
            logger.warn("No valid JWT token found in request for logout.");
            return ResponseEntity.status(ErrorCode.NO_VALID_TOKEN.getHttpStatus())
                    .body(new CustomErrorException(
                            ErrorCode.NO_VALID_TOKEN.getHttpStatus(),
                            ErrorCode.NO_VALID_TOKEN.getMessage(),
                            ErrorPath.GET_LOGOUT_API.getPath()
                    ));
        }
    }
}
