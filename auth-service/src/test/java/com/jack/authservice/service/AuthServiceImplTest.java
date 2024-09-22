package com.jack.authservice.service.impl;

import com.jack.authservice.client.UserServiceClient;
import com.jack.authservice.security.JwtTokenProvider;
import com.jack.authservice.service.AuthService;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
    }

    @Test
    void login_Success() {
        // Arrange
        AuthRequestDto authRequestDto = new AuthRequestDto("test@example.com", "password");

        // Simulate user-service successfully validating password
        when(userServiceClient.verifyPassword(any(AuthRequestDto.class))).thenReturn(true);

        // Simulate JWT token creation
        when(jwtTokenProvider.generateTokenFromEmail(anyString())).thenReturn("jwt-token");

        // Act
        AuthResponseDto authResponse = authService.login(authRequestDto);

        // Assert
        assertNotNull(authResponse);
        assertEquals("jwt-token", authResponse.getToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(60 * 60 * 1000L, authResponse.getExpiresIn());

        // Verify that the client and provider were called correctly
        verify(userServiceClient, times(1)).verifyPassword(any(AuthRequestDto.class));
        verify(jwtTokenProvider, times(1)).generateTokenFromEmail(anyString());
    }

    @Test
    void login_Failure_InvalidCredentials() {
        // Arrange
        AuthRequestDto authRequestDto = new AuthRequestDto("test@example.com", "wrong-password");

        // Simulate user-service returning false for password validation
        when(userServiceClient.verifyPassword(any(AuthRequestDto.class))).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(authRequestDto);
        });

        assertEquals("Invalid username or password.", exception.getMessage());

        // Verify that the client was called once and the token provider was not called
        verify(userServiceClient, times(1)).verifyPassword(any(AuthRequestDto.class));
        verify(jwtTokenProvider, never()).generateTokenFromEmail(anyString());
    }
}
