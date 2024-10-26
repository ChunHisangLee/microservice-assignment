package com.jack.userservice.dto;

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
public class UserResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String email;
    private String token;
}
