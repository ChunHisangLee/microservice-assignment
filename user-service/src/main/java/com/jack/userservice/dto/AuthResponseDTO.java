package com.jack.userservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String tokenType;  // Typically "Bearer"
    private Long expiresIn;  // Optional: Token expiration time (in milliseconds)
}
