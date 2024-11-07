package com.jack.authservice.security;

import com.jack.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Log4j2
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(JwtSecretKeyProvider secretKeyProvider) {
        this.secretKey = secretKeyProvider.getSecretKey();
        this.jwtExpirationMs = SecurityConstants.JWT_EXPIRATION_MS;
        log.info("Initialized JwtTokenProvider with expiration time: {} ms", jwtExpirationMs);
    }

    public String generateTokenFromEmail(String email) {
        log.info("Generating JWT token for email: {}", email);
        String token = Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(secretKey)
                .compact();

        log.info("Generated JWT token successfully for email: {}", email);
        return token;
    }


    // Extract email from the JWT token
    public String getEmailFromToken(String token) {
        log.debug("Extracting email from token: {}", token);
        String email = getClaimsFromToken(token).getSubject();
        log.info("Extracted email: {}", email);
        return email;
    }

    // Extract all claims from the JWT token
    public Claims getClaimsFromToken(String token) {
        log.debug("Parsing claims from token");
        JwtParser parser = Jwts.parser()
                .verifyWith(secretKey)
                .build();

        // Parse the claims from the token
        Claims claims = parser.parseSignedClaims(token).getPayload();
        log.debug("Parsed claims successfully");
        return claims;
    }

    // Validate the JWT token
    public boolean validateToken(String token) {
        log.info("Validating JWT token: {}", token);

        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .build();

            // Parse and validate the token (will throw an exception if invalid)
            parser.parse(token);
            log.info("JWT token is valid");
            return true;
        } catch (Exception ex) {
            System.err.println("Invalid JWT token: " + ex.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        log.info("Creating authentication object from JWT token");
        // Extract the email (not username) from the token
        String email = getEmailFromToken(token);
        log.info("Creating Authentication object for email: {}", email);
        return new UsernamePasswordAuthenticationToken(email, null, null);  // No credentials or authorities
    }
}
