package com.jack.common.dto.request;

import lombok.*;

@Getter
@Setter
public class UserRequestDto {
    private String name;
    private String email;
    private String password;
}
