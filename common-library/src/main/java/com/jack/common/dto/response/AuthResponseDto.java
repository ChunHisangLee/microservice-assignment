package com.jack.common.dto.response;
import lombok.*;

@Getter
@Setter
@Builder
public class AuthResponseDto {
    private String token;
    private String tokenType;  // Typically "Bearer"
    private Long expiresIn;  // Optional: Token expiration time (in milliseconds)
}
