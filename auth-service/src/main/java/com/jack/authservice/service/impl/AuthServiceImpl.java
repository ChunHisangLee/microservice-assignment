package com.jack.authservice.service.impl;

import com.jack.authservice.client.UserServiceClient;
import com.jack.authservice.security.JwtTokenProvider;
import com.jack.authservice.service.AuthService;
import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements AuthService {
    private final UserServiceClient userServiceClient;  // Inject Feign client
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponseDto login(AuthRequestDto authRequestDto) {
        log.info("Attempting to authenticate user with email: {}", authRequestDto.getEmail());
        // Delegate password validation to the user-service
        boolean isPasswordValid;

        try {
            isPasswordValid = userServiceClient.verifyPassword(authRequestDto);
        } catch (Exception e) {
            log.error("Failed to verify password due to an error: {}", e.getMessage());
            throw new BadCredentialsException("Authentication service unavailable.");
        }

        if (!isPasswordValid) {
            log.error("Invalid credentials for user: {}", authRequestDto.getEmail());
            throw new BadCredentialsException("Invalid username or password.");
        }

        // If valid, generate JWT token
        String jwt = jwtTokenProvider.generateTokenFromEmail(authRequestDto.getEmail());
        log.info("Generated JWT token for user: {}", authRequestDto.getEmail());
        return AuthResponseDto.builder()

                .token(jwt)
                .tokenType("Bearer")  // Default token type is Bearer
                .expiresIn(SecurityConstants.JWT_EXPIRATION_MS)  // 1-hour expiration time
                .build();
    }
}
