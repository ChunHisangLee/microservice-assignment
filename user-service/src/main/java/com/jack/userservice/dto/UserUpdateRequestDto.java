package com.jack.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserUpdateRequestDto {
    private Long id;
    private String name;
    private String email;
    private String password;
}
