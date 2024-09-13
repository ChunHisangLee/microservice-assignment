package com.jack.authservice.service;

import com.jack.authservice.dto.AuthRequestDTO;
import com.jack.authservice.dto.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO login(AuthRequestDTO authRequestDTO);;
}
