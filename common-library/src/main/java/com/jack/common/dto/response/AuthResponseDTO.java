package com.jack.common.dto.response;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String tokenType;  // Typically "Bearer"
    private Long expiresIn;  // Optional: Token expiration time (in milliseconds)
}
