package com.jack.authservice.service.impl;

import com.jack.common.constants.SecurityConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Initialize the secret key based on SecurityConstants
        secretKey = Keys.hmacShaKeyFor(SecurityConstants.JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        tokenService.init();  // Initialize secretKey in the service
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * Helper method to generate JWT tokens for testing.
     *
     * @param userId     The user ID to embed in the token's subject.
     * @param ttlSeconds Time-to-live in seconds for the token's expiration.
     * @return A signed JWT token as a String.
     */
    private String generateToken(Long userId, long ttlSeconds) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + ttlSeconds * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Test the successful invalidation of a token.
     * Verifies that the token is added to Redis with the correct TTL.
     */
    @Test
    void testInvalidateToken_Success() {
        // Generate a token for userId=1 with a TTL of 3600 seconds (1 hour)
        String token = generateToken(1L, 3600L); // userId=1, 1 hour TTL

        // Simulate that the token is not already blacklisted in Redis
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        // Perform the token invalidation
        tokenService.invalidateToken(token);

        // Verify that the token is added to Redis with the correct key and value
        verify(valueOperations, times(1)).set(SecurityConstants.BLACKLIST_PREFIX + token, token);

        // Verify that the 'expire' method is called with the correct key, TTL, and TimeUnit
        verify(redisTemplate, times(1)).expire(
                eq(SecurityConstants.BLACKLIST_PREFIX + token), // Use eq() matcher for the key
                anyLong(),                                       // Use anyLong() matcher for TTL
                eq(TimeUnit.SECONDS)                             // Use eq() matcher for TimeUnit
        );
    }


    /**
     * Test invalidating a token that is already blacklisted.
     * Ensures that no additional actions are taken.
     */
    @Test
    void testInvalidateToken_AlreadyBlacklisted() {
        String token = "blacklistedToken";

        // Simulate that the token is already blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(true);

        // Perform the invalidation
        tokenService.invalidateToken(token);

        // Verify that no further actions are taken
        verify(valueOperations, never()).set(anyString(), anyString());
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    /**
     * Test invalidating an invalid or empty token.
     * Expects an IllegalArgumentException to be thrown.
     */
    @Test
    void testInvalidateToken_InvalidToken() {
        // Test with empty token
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> tokenService.invalidateToken(""));
        assertEquals("Token is invalid or empty.", exception1.getMessage());

        // Test with null token
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> tokenService.invalidateToken(null));
        assertEquals("Token is invalid or empty.", exception2.getMessage());

        // Verify that no actions are taken on invalid tokens
        verify(valueOperations, never()).set(anyString(), anyString());
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    /**
     * Test checking if a token is blacklisted.
     * Verifies that the method returns true when the token is blacklisted.
     */
    @Test
    void testIsTokenBlacklisted_True() {
        String token = "blacklistedToken";

        // Simulate that the token is blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(true);

        boolean result = tokenService.isTokenBlacklisted(token);

        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Test checking if a token is blacklisted.
     * Verifies that the method returns false when the token is not blacklisted.
     */
    @Test
    void testIsTokenBlacklisted_False() {
        String token = "validToken";

        // Simulate that the token is not blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        boolean result = tokenService.isTokenBlacklisted(token);

        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Test validating a token successfully.
     * Verifies that a valid token with matching userId returns true.
     */
    @Test
    void testValidateToken_Success() {
        Long userId = 1L;
        String token = generateToken(userId, 3600L); // userId=1, 1 hour TTL

        // Simulate that the token is not blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        boolean isValid = tokenService.validateToken(token, userId);

        assertTrue(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Test validating a token that is blacklisted.
     * Verifies that a blacklisted token returns false.
     */
    @Test
    void testValidateToken_Failed_Blacklisted() {
        String token = "blacklistedToken";
        Long userId = 1L;

        // Simulate that the token is blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(true);

        boolean isValid = tokenService.validateToken(token, userId);

        assertFalse(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Test validating an invalid token.
     * Verifies that an invalid token returns false.
     */
    @Test
    void testValidateToken_Failed_InvalidToken() {
        String token = "invalidToken";
        Long userId = 1L;

        // Simulate that the token is not blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        boolean isValid = tokenService.validateToken(token, userId);

        assertFalse(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Test validating a token with a mismatched userId.
     * Verifies that the method returns false when userId does not match.
     */
    @Test
    void testValidateToken_Failed_DifferentUserId() {
        Long tokenUserId = 2L;
        Long providedUserId = 1L;
        String token = generateToken(tokenUserId, 3600L); // Token for userId=2

        // Simulate that the token is not blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        boolean isValid = tokenService.validateToken(token, providedUserId);

        assertFalse(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Test validating an expired token.
     * Verifies that an expired token returns false.
     */
    @Test
    void testValidateToken_Failed_ExpiredToken() {
        Long userId = 1L;
        String token = generateToken(userId, -3600L); // Token expired 1 hour ago

        // Simulate that the token is not blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        boolean isValid = tokenService.validateToken(token, userId);

        assertFalse(isValid);
        verify(redisTemplate, times(1)).hasKey(SecurityConstants.BLACKLIST_PREFIX + token);
    }

    /**
     * Since getTokenExpiryDuration is a private method, we do not test it directly.
     * Instead, we ensure that methods like invalidateToken behave correctly, which indirectly tests the expiry duration logic.
     */

    /**
     * Additional test to ensure that invalidateToken correctly handles tokens with various expiry durations.
     * For example, tokens that are already expired should not be added to the blacklist with a negative TTL.
     */
    @Test
    void testInvalidateToken_TokenAlreadyExpired() {
        Long userId = 1L;
        String token = generateToken(userId, -3600L); // Token expired 1 hour ago

        // Simulate that the token is not already blacklisted
        when(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token)).thenReturn(false);

        // Perform the invalidation
        tokenService.invalidateToken(token);

        // Since the token is already expired, TTL should be 0
        verify(valueOperations, times(1)).set(SecurityConstants.BLACKLIST_PREFIX + token, token);
        verify(redisTemplate, times(1)).expire(SecurityConstants.BLACKLIST_PREFIX + token, 0L, TimeUnit.SECONDS);
    }
}
