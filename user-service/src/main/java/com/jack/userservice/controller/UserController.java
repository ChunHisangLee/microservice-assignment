package com.jack.userservice.controller;

import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.constants.ErrorMessages;
import com.jack.userservice.constants.SecurityConstants;
import com.jack.userservice.dto.*;
import com.jack.userservice.entity.Users;
import com.jack.userservice.exception.CustomErrorException;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.service.UserService;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jack.userservice.constants.ErrorMessages.GET_USER_API_PATH;
import static com.jack.userservice.constants.ErrorMessages.USER_NOT_FOUND;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UsersMapper usersMapper;
    private final AuthServiceClient authServiceClient;

    public UserController(UserService userService, UsersMapper usersMapper, AuthServiceClient authServiceClient) {
        this.userService = userService;
        this.usersMapper = usersMapper;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody UserRegistrationDTO userRegistrationDTO) {
        UserResponseDTO userResponse = userService.register(userRegistrationDTO);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@RequestBody AuthRequestDTO authRequestDTO) {
        boolean isPasswordValid = userService.verifyPassword(authRequestDTO.getEmail(), authRequestDTO.getPassword());
        return ResponseEntity.ok(isPasswordValid);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsersDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UsersDTO usersDTO,
            @RequestHeader("Authorization") String token) {

        validateToken(token, id, ErrorMessages.PUT_USER_API_PATH);
        logger.info("Updating user with ID: {}", id);
        Users users = usersMapper.toEntity(usersDTO);
        Users updatedUser = userService.updateUser(id, users)
                .orElseThrow(() -> {
                    logger.error("User with ID: {} not found for update.", id);
                    return new CustomErrorException(
                            HttpStatus.NOT_FOUND,
                            USER_NOT_FOUND,
                            GET_USER_API_PATH + id
                    );
                });

        logger.info("User with ID: {} updated successfully.", id);
        return ResponseEntity.ok(usersMapper.toDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        validateToken(token, id, ErrorMessages.DELETE_USER_API_PATH);
        logger.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        logger.info("User with ID: {} deleted successfully.", id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsersDTO> getUserById(@PathVariable Long id) {
        logger.info("Fetching user with ID: {}", id);
        UsersDTO userDTO = userService.getUserWithBalance(id);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO loginRequest) {
        logger.info("User login attempt with email: {}", loginRequest.getEmail());

        if (!userService.verifyPassword(loginRequest.getEmail(), loginRequest.getPassword())) {
            logger.error("Invalid credentials for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AuthResponseDTO authResponse = authServiceClient.login(loginRequest);
        logger.info("User with email: {} logged in successfully.", loginRequest.getEmail());
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        if (token != null && token.startsWith(SecurityConstants.BEARER_PREFIX)) {
            try {
                authServiceClient.logout(token);
                logger.info("Logout request sent to auth-service with token: {}", token);
                return ResponseEntity.ok().build();
            } catch (FeignException e) {
                logger.error("Error during logout via auth-service: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            logger.warn("No valid JWT token found in request for logout.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private void validateToken(String token, Long userId, String path) {
        if (!authServiceClient.validateToken(token, userId)) {
            logger.error("Unauthorized request for user with ID: {}", userId);
            throw new CustomErrorException(HttpStatus.UNAUTHORIZED, ErrorMessages.UNAUTHORIZED_REQUEST, path + userId);
        }
    }
}
