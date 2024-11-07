package com.jack.authservice.service.impl;

import com.jack.authservice.security.JwtTokenProvider;
import com.jack.authservice.service.TokenService;
import com.jack.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
public class TokenServiceImpl implements TokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenServiceImpl(RedisTemplate<String, String> redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
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

        long tokenExpiryDuration = calculateTokenExpiryDuration(token);
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

        if (!jwtTokenProvider.validateToken(token)) {
            log.error("Invalid JWT token");
            return false;
        }

        String tokenSubject = jwtTokenProvider.getEmailFromToken(token);

        if (tokenSubject == null) {
            log.error("Failed to extract subject from token");
            return false;
        }

        Long tokenUserId = Long.parseLong(tokenSubject);
        return tokenUserId.equals(userId);
    }

    private long calculateTokenExpiryDuration(String token) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        if (claims == null) {
            log.error("Failed to parse claims from token");
            return 0;
        }

        Date expiration = claims.getExpiration();

        if (expiration == null) {
            log.warn("Token does not have an expiration date set.");
            return 0;
        }

        long now = System.currentTimeMillis();
        long timeToExpiry = (expiration.getTime() - now) / 1000;
        return Math.max(timeToExpiry, 0);
    }
}
