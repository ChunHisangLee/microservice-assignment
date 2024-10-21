package com.jack.authservice.service.impl;

import com.jack.authservice.service.TokenService;
import com.jack.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
public class TokenServiceImpl implements TokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private SecretKey secretKey;

    public TokenServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // Initialize the SecretKey using the JWT secret key from SecurityConstants
        this.secretKey = Keys.hmacShaKeyFor(SecurityConstants.JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void invalidateToken(@NonNull String token) {
        if (token.trim().isEmpty()) {
            log.warn("Invalid token provided for invalidation.");
            throw new IllegalArgumentException("Token is invalid or empty.");
        }

        Boolean isTokenBlacklisted = redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token);

        if (Boolean.TRUE.equals(isTokenBlacklisted)) {
            log.info("Token is already blacklisted: {}", token);
            return;  // Exit early if token is already blacklisted
        }

        long tokenExpiryDuration = getTokenExpiryDuration(token);
        redisTemplate.opsForValue().set(SecurityConstants.BLACKLIST_PREFIX + token, token);
        redisTemplate.expire(SecurityConstants.BLACKLIST_PREFIX + token, tokenExpiryDuration, TimeUnit.SECONDS);
        log.info("Token added to blacklist with TTL: {} seconds", tokenExpiryDuration);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SecurityConstants.BLACKLIST_PREFIX + token));
    }

    @Override
    public boolean validateToken(String token, Long userId) {
        if (isTokenBlacklisted(token)) {
            log.warn("Token is blacklisted: {}", token);
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
            log.error("The Token is invalid: {}", e.getMessage());
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
            long timeToExpiry = (expiration.getTime() - now) / 1000;

            if (timeToExpiry <= 0) {
                log.warn("Token has already expired.");
                return 0;
            }

            return timeToExpiry;
        } catch (Exception e) {
            log.error("Failed to parse JWT token to get expiry duration: {}", e.getMessage());
            return 0;
        }
    }
}
