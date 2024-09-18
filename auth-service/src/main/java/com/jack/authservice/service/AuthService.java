package com.jack.authservice.service;

import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO login(AuthRequestDto authRequestDTO);;
}
