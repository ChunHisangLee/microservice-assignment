package com.jack.authservice.service;

import com.jack.common.dto.AuthRequestDto;
import com.jack.common.dto.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO login(AuthRequestDto authRequestDTO);;
}
