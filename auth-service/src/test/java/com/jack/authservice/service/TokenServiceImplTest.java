package com.jack.authservice.service;

import com.jack.authservice.service.impl.TokenServiceImpl;
import com.jack.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TokenServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // No specific setup required, MockMvc and MockBean do the work
    }

    @Test
    void testInvalidateToken_Success() {
        // Arrange
        String token = "valid-jwt-token";
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // Token not in blacklist
        doNothing().when(redisTemplate).opsForValue().set(anyString(), anyString());
        doNothing().when(redisTemplate).expire(anyString(), anyLong(), any());

        // Act
        tokenService.invalidateToken(token);

        // Assert
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
        verify(redisTemplate, times(1)).opsForValue().set(SecurityConstants.BLACKLIST_PREFIX + token, token);
    }

    @Test
    void testInvalidateToken_AlreadyBlacklisted() {
        // Arrange
        String token = "blacklisted-jwt-token";
        when(redisTemplate.hasKey(anyString())).thenReturn(true); // Token already in blacklist

        // Act
        tokenService.invalidateToken(token);

        // Assert
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
        verify(redisTemplate, never()).opsForValue().set(anyString(), anyString());
    }

    @Test
    void testIsTokenBlacklisted_True() {
        // Arrange
        String token = "blacklisted-jwt-token";
        when(redisTemplate.hasKey(anyString())).thenReturn(true); // Token is in blacklist

        // Act
        boolean isBlacklisted = tokenService.isTokenBlacklisted(token);

        // Assert
        assertTrue(isBlacklisted);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    @Test
    void testIsTokenBlacklisted_False() {
        // Arrange
        String token = "valid-jwt-token";
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // Token is not in blacklist

        // Act
        boolean isBlacklisted = tokenService.isTokenBlacklisted(token);

        // Assert
        assertFalse(isBlacklisted);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    @Test
    void testValidateToken_Success() {
        // Arrange
        String token = "valid-jwt-token";
        Long userId = 1L;
        Claims claims = mock(Claims.class);
        Jwt<Claims, Claims> jwt = mock(Jwt.class);
        JwtParser parser = mock(JwtParser.class);

        // Mock JWT parsing behavior
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwt.getPayload()).thenReturn(claims);
        when(parser.parse(anyString())).thenReturn(jwt);
        when(Jwts.parser().verifyWith(secretKey).build()).thenReturn(parser);

        // Mock Redis response to show the token is not blacklisted
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        boolean isValid = tokenService.validateToken(token, userId);

        // Assert
        assertTrue(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    @Test
    void testValidateToken_Blacklisted() {
        // Arrange
        String token = "blacklisted-jwt-token";
        Long userId = 1L;

        // Mock Redis response to show the token is blacklisted
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        boolean isValid = tokenService.validateToken(token, userId);

        // Assert
        assertFalse(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Arrange
        String token = "invalid-jwt-token";
        Long userId = 1L;

        // Mock JWT parsing behavior to throw an exception (invalid token)
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(Jwts.parser().verifyWith(secretKey).build()).thenThrow(new RuntimeException("Invalid Token"));

        // Act
        boolean isValid = tokenService.validateToken(token, userId);

        // Assert
        assertFalse(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }
}
