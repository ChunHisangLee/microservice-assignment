package com.jack.authservice.service.impl;

import com.jack.authservice.client.UserServiceClient;
import com.jack.authservice.security.JwtTokenProvider;
import com.jack.authservice.service.AuthService;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class AuthServiceImpl implements AuthService {
    private final UserServiceClient userServiceClient;  // Inject Feign client
    private final JwtTokenProvider jwtTokenProvider;
    private static final Long JWT_EXPIRATION_MS = 60 * 60 * 1000L;

    public AuthServiceImpl(UserServiceClient userServiceClient, JwtTokenProvider jwtTokenProvider) {
        this.userServiceClient = userServiceClient;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponseDto login(AuthRequestDto authRequestDto) {
        log.info("Attempting to authenticate user with email: {}", authRequestDto.getEmail());
        // Delegate password validation to the user-service
        boolean isPasswordValid = userServiceClient.verifyPassword(authRequestDto);

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
                .expiresIn(JWT_EXPIRATION_MS)  // 1-hour expiration time
                .build();
    }
}
