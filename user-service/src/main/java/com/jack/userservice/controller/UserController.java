package com.jack.userservice.controller;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.request.UserRegistrationRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.common.dto.response.UserResponseDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;
import com.jack.userservice.service.UserService;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Log4j2
public class UserController {
    private final UserService userService;
    private final AuthServiceClient authServiceClient;

    public UserController(UserService userService, AuthServiceClient authServiceClient) {
        this.userService = userService;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationRequestDto userRegistrationRequestDto) {
        UserResponseDto userResponse = userService.register(userRegistrationRequestDto);
        return ResponseEntity.status(201)
                .body(userResponse);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@Valid @RequestBody AuthRequestDto authRequestDto) {
        boolean isPasswordValid = userService.verifyPassword(authRequestDto.getEmail(), authRequestDto.getPassword());
        return ResponseEntity.ok(isPasswordValid);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDto loginRequest) {
        log.info("User login attempt with email: {}", loginRequest.getEmail());

        if (!userService.verifyPassword(loginRequest.getEmail(), loginRequest.getPassword())) {
            log.error("Invalid credentials for email: {}", loginRequest.getEmail());
            throw new CustomErrorException(ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus(),
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getMessage(),
                    ErrorPath.POST_LOGIN_API.getPath());
        }

        try {
            AuthResponseDto authResponse = authServiceClient.login(loginRequest);
            log.info("User with email: {} logged in successfully.", loginRequest.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (FeignException e) {
            log.error("Error during login via auth-service: {}", e.getMessage());
            throw new CustomErrorException(ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus(),
                    ErrorCode.LOGOUT_SERVICE_ERROR.getMessage(),
                    ErrorPath.POST_LOGIN_API.getPath());
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        if (token != null && token.startsWith(SecurityConstants.BEARER_PREFIX)) {
            try {
                authServiceClient.logout(token);
                log.info("Logout request sent to auth-service.");
                return ResponseEntity.ok()
                        .build();
            } catch (FeignException e) {
                log.error("Error during logout via auth-service: {}", e.getMessage());
                throw new CustomErrorException(ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus(),
                        ErrorCode.LOGOUT_SERVICE_ERROR.getMessage(),
                        ErrorPath.GET_LOGOUT_API.getPath());
            }
        } else {
            log.warn("No valid JWT token found in request for logout.");
            throw new CustomErrorException(ErrorCode.NO_VALID_TOKEN.getHttpStatus(),
                    ErrorCode.NO_VALID_TOKEN.getMessage(),
                    ErrorPath.GET_LOGOUT_API.getPath());
        }
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<UsersDto> getUserWithBalance(@PathVariable Long userId) {
        UsersDto userWithBalance = userService.getUserWithBalance(userId);
        return ResponseEntity.ok(userWithBalance);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable Long id, @Valid @RequestBody Users users) {
        return userService.updateUser(id, users)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent()
                .build();
    }
}
