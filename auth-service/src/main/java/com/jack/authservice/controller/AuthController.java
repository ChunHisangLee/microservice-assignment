package com.jack.authservice.controller;

import com.jack.authservice.service.AuthService;
import com.jack.authservice.service.TokenService;
import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody AuthRequestDto authRequestDTO) {
        AuthResponseDto authResponse = authService.login(authRequestDTO);
        log.info("User {} logged in successfully", authRequestDTO.getEmail());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String token) {
        String jwtToken = extractToken(token);

        if (jwtToken == null) {
            log.warn("Invalid token provided for logout.");
            return ResponseEntity.badRequest().build();
        }

        try {
            tokenService.invalidateToken(jwtToken);
            log.info("Token invalidated successfully: {}", jwtToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to invalidate token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String token,
            @RequestParam Long userId) {
        String jwtToken = extractToken(token);
        if (jwtToken == null) {
            log.warn("Invalid token format.");
            return ResponseEntity.badRequest().body(false);
        }

        try {
            boolean isValid = tokenService.validateToken(token, userId);

            if (isValid) {
                log.info("Token is valid for userId: {}", userId);
                return ResponseEntity.ok(true);
            } else {
                log.warn("Token is invalid or does not match userId: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    private String extractToken(String token) {
        if (token != null && token.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return token.substring(SecurityConstants.PREFIX_INDEX);
        }

        return null;
    }
}
