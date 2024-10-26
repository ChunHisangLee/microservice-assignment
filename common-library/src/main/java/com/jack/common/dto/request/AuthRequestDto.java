package com.jack.common.dto.request;

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
public class AuthRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String email;
    private String password;
}
