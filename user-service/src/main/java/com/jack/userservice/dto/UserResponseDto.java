package com.jack.userservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String email;
    private String token;
}
