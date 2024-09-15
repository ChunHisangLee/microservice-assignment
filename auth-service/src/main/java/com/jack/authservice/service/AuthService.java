package com.jack.authservice.service;

import com.jack.common.dto.AuthRequestDTO;
import com.jack.common.dto.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO login(AuthRequestDTO authRequestDTO);;
}
