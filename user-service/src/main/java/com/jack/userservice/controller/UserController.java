package com.jack.userservice.controller;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.request.UserRegistrationRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.dto.UserResponseDto;
import com.jack.userservice.dto.UserUpdateRequestDto;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.service.UserService;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Log4j2
public class UserController {
    private final UserService userService;
    private final AuthServiceClient authServiceClient;

    @Autowired
    public UserController(UserService userService, AuthServiceClient authServiceClient) {
        this.userService = userService;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationRequestDto userRegistrationRequestDto) {
        UserResponseDto userResponse = userService.register(userRegistrationRequestDto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(userResponse.getId())
                .toUri();

        log.info("User registered successfully with ID: {}", userResponse.getId());

        return ResponseEntity.created(location)
                .body(userResponse);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@Valid @RequestBody AuthRequestDto authRequestDto) {
        boolean isPasswordValid = userService.isPasswordValid(authRequestDto.getEmail(), authRequestDto.getPassword());
        log.debug("Password verification for email {}: {}", authRequestDto.getEmail(), isPasswordValid);
        return ResponseEntity.ok(isPasswordValid);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto loginRequest) {
        log.info("User login attempt with email: {}", loginRequest.getEmail());

        if (!userService.isPasswordValid(loginRequest.getEmail(), loginRequest.getPassword())) {
            log.warn("Invalid credentials for email: {}", loginRequest.getEmail());
            throw new CustomErrorException(
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus(),
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getMessage(),
                    ErrorPath.POST_LOGIN_API.getPath()
            );
        }

        try {
            AuthResponseDto authResponse = authServiceClient.login(loginRequest);
            log.info("User with email: {} logged in successfully.", loginRequest.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (FeignException e) {
            log.error("Error during login via auth-service: {}", e.getMessage());
            throw new CustomErrorException(
                    ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus(),
                    ErrorCode.LOGOUT_SERVICE_ERROR.getMessage(),
                    ErrorPath.POST_LOGIN_API.getPath()
            );
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());

            try {
                authServiceClient.logout(token);
                log.info("Logout request sent to auth-service for token: {}", maskToken(token));
                return ResponseEntity.ok()
                        .build();
            } catch (FeignException e) {
                log.error("Error during logout via auth-service: {}", e.getMessage());
                throw new CustomErrorException(
                        ErrorCode.LOGOUT_SERVICE_ERROR.getHttpStatus(),
                        ErrorCode.LOGOUT_SERVICE_ERROR.getMessage(),
                        ErrorPath.GET_LOGOUT_API.getPath()
                );
            }
        } else {
            log.warn("No valid JWT token found in request for logout.");
            throw new CustomErrorException(
                    ErrorCode.NO_VALID_TOKEN.getHttpStatus(),
                    ErrorCode.NO_VALID_TOKEN.getMessage(),
                    ErrorPath.GET_LOGOUT_API.getPath()
            );
        }
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<UsersDto> getUserWithBalance(@PathVariable Long userId) {
        Optional<UsersDto> userWithBalanceOpt = userService.getUserWithBalance(userId);

        if (userWithBalanceOpt.isEmpty()) {
            log.warn("User not found with ID in getUserWithBalance: {}", userId);
            throw new CustomErrorException(
                    ErrorCode.USER_NOT_FOUND.getHttpStatus(),
                    ErrorCode.USER_NOT_FOUND.getMessage(),
                    ErrorPath.GET_USER_BALANCE_API.getPath()
            );
        }

        UsersDto userWithBalance = userWithBalanceOpt.get();
        log.info("Retrieved balance for user ID: {}", userId);
        return ResponseEntity.ok(userWithBalance);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        Optional<UserResponseDto> updatedUserOpt = userService.updateUser(id, userUpdateRequestDto);

        return updatedUserOpt.map(updatedUser -> {
            log.info("User with ID: {} updated successfully.", id);
            return ResponseEntity.ok(updatedUser);
        }).orElseThrow(() -> {
            log.warn("User not found with ID in updateUser: {}", id);
            return new CustomErrorException(
                    ErrorCode.USER_NOT_FOUND.getHttpStatus(),
                    ErrorCode.USER_NOT_FOUND.getMessage(),
                    ErrorPath.PUT_UPDATE_USER_API.getPath()
            );
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            log.info("User with ID: {} deleted successfully.", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("Attempted to delete non-existent user with ID: {}", id);
            throw new CustomErrorException(
                    ErrorCode.USER_NOT_FOUND.getHttpStatus(),
                    ErrorCode.USER_NOT_FOUND.getMessage(),
                    ErrorPath.DELETE_USER_API.getPath()
            );
        }
    }

    private String maskToken(String token) {
        if (token.length() <= 10) {
            return "*****";
        }

        return token.substring(0, 5) + "*****" + token.substring(token.length() - 5);
    }
}
