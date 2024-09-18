package com.jack.authservice.service.impl;

import com.jack.authservice.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {
    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    private SecretKey secretKey;

    public TokenServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // Initialize the SecretKey after the jwtSecret has been injected
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void invalidateToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            // Check if token is already in the blacklist
            Boolean isTokenBlacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            if (Boolean.TRUE.equals(isTokenBlacklisted)) {
                logger.info("Token is already blacklisted: {}", token);
                return;  // Exit early if token is already blocklisted
            }

            // Get token expiration duration
            long tokenExpiryDuration = getTokenExpiryDuration(token);

            // Save the token in Redis with a TTL (Time-to-Live)
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, token);
            redisTemplate.expire(BLACKLIST_PREFIX + token, tokenExpiryDuration, TimeUnit.SECONDS);
            logger.info("Token added to blacklist with TTL: {} seconds", tokenExpiryDuration);
        } else {
            logger.warn("Invalid token provided for invalidation.");
            throw new IllegalArgumentException("Token is invalid or empty.");
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    @Override
    public boolean validateToken(String token, Long userId) {
        if (isTokenBlacklisted(token)) {
            logger.warn("Token is blacklisted: {}", token);
            return false;
        }

        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .build();

            Claims claims = parser.parseSignedClaims(token).getPayload();

            Long tokenUserId = Long.parseLong(claims.getSubject());
            return tokenUserId.equals(userId);
        } catch (Exception e) {
            logger.error("The Token is invalid: {}", e.getMessage());
            return false;
        }
    }

    private long getTokenExpiryDuration(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .build();

            Claims claims = parser.parseSignedClaims(token).getPayload();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            long timeToExpiry = (expiration.getTime() - now) / 1000;  // Convert milliseconds to seconds

            // If the token is already expired or has less than 0 seconds, return 0
            if (timeToExpiry <= 0) {
                logger.warn("Token has already expired.");
                return 0;
            }

            return timeToExpiry;
        } catch (Exception e) {
            logger.error("Failed to parse JWT token to get expiry duration: {}", e.getMessage());
            return jwtExpirationMs / 1000;  // Return default expiration time in case of parsing failure
        }
    }
}
