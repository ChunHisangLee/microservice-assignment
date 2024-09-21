package com.jack.common.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserRegistrationRequestDto {
    private String name;
    private String email;
    private String password;
}
