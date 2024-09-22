package com.jack.authservice.service;

import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;

public interface AuthService {
    AuthResponseDto login(AuthRequestDto authRequestDTO);
}
