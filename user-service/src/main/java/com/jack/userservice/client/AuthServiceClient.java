package com.jack.userservice.client;

import com.jack.userservice.constants.SecurityConstants;
import com.jack.userservice.dto.AuthRequestDTO;
import com.jack.userservice.dto.AuthResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", url = "${AUTH_SERVICE_URL:https://auth-service:8084}")
public interface AuthServiceClient {

    @PostMapping("/api/auth/login")
    AuthResponseDTO login(@RequestBody AuthRequestDTO authRequestDTO);

    @PostMapping("/api/auth/logout")
    void logout(@RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String token);

    @PostMapping("/api/auth/validate")
    Boolean validateToken(@RequestHeader(SecurityConstants.AUTHORIZATION_HEADER) String token, @RequestParam Long userId);

}
