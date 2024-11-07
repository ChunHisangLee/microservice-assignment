package com.jack.authservice.security;

import com.jack.common.constants.SecurityConstants;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Getter
@Component
public class JwtSecretKeyProvider {
    private final SecretKey secretKey;

    public JwtSecretKeyProvider() {
        this.secretKey = Keys.hmacShaKeyFor(SecurityConstants.JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

}
