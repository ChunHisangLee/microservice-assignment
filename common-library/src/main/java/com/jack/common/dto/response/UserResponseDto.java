package com.jack.common.dto.response;

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
