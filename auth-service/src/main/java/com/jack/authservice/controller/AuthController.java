package com.jack.authservice.controller;

import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.authservice.service.AuthService;
import com.jack.authservice.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody AuthRequestDto authRequestDTO) {
        AuthResponseDto authResponse = authService.login(authRequestDTO);
        logger.info("User {} logged in successfully", authRequestDTO.getEmail());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String token) {
        if (token != null && token.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String jwtToken = token.substring(7);  // Remove "Bearer " prefix

            try {
                tokenService.invalidateToken(jwtToken);
                logger.info("Token invalidated successfully: {}", jwtToken);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                logger.error("Failed to invalidate token: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            logger.warn("Invalid token provided for logout.");
            return ResponseEntity.badRequest().build();  // HTTP 400 Bad Request if no token found
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String token,
            @RequestParam Long userId) {

        if (token != null && token.startsWith(SecurityConstants.BEARER_PREFIX)) {
            token = token.substring(7);  // Remove "Bearer " prefix

            try {
                boolean isValid = tokenService.validateToken(token, userId);

                if (isValid) {
                    logger.info("Token is valid for userId: {}", userId);
                    return ResponseEntity.ok(true);
                } else {
                    logger.warn("Token is invalid or does not match userId: {}", userId);
                    return ResponseEntity.status(401).body(false);  // Unauthorized if token is invalid
                }
            } catch (Exception e) {
                logger.error("Error validating token: {}", e.getMessage());
                return ResponseEntity.status(500).body(false);  // Internal server error in case of an exception
            }
        } else {
            logger.warn("Invalid token format.");
            return ResponseEntity.badRequest().body(false);  // Bad Request if a token format is incorrect
        }
    }
}
