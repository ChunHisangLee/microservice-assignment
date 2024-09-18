package com.jack.authservice.service.impl;

import com.jack.authservice.client.UserServiceClient;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDTO;
import com.jack.authservice.security.JwtTokenProvider;
import com.jack.authservice.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserServiceClient userServiceClient;  // Inject Feign client
    private final JwtTokenProvider jwtTokenProvider;
    // Define the token expiration duration (e.g., 1 hour)
    private static final Long JWT_EXPIRATION_MS = 3600000L;

    public AuthServiceImpl(UserServiceClient userServiceClient, JwtTokenProvider jwtTokenProvider) {
        this.userServiceClient = userServiceClient;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponseDTO login(AuthRequestDto authRequestDTO) {
        logger.info("Attempting to authenticate user with email: {}", authRequestDTO.getEmail());
        // Delegate password validation to the user-service
        boolean isPasswordValid = userServiceClient.verifyPassword(authRequestDTO);

        if (!isPasswordValid) {
            logger.error("Invalid credentials for user: {}", authRequestDTO.getEmail());
            throw new BadCredentialsException("Invalid username or password.");
        }

        // If valid, generate JWT token
        String jwt = jwtTokenProvider.generateTokenFromEmail(authRequestDTO.getEmail());
        logger.info("Generated JWT token for user: {}", authRequestDTO.getEmail());
        return AuthResponseDTO.builder()
                .token(jwt)
                .tokenType("Bearer")  // Default token type is Bearer
                .expiresIn(JWT_EXPIRATION_MS)  // 1-hour expiration time
                .build();
    }
}
