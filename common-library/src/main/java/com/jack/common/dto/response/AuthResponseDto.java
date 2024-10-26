package com.jack.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String token;
    private String tokenType;  // Typically "Bearer"
    private Long expiresIn;  // Optional: Token expiration time (in milliseconds)
}
