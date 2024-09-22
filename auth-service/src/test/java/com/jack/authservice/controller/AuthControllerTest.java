package com.jack.authservice.controller;

import com.jack.authservice.service.AuthService;
import com.jack.authservice.service.TokenService;
import com.jack.common.constants.SecurityConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        // No specific setup required, MockMvc and MockBean do the work
    }

    @Test
    void testLogin_Success() throws Exception {
        // Mock input and output
        AuthRequestDto authRequestDto = new AuthRequestDto("test@example.com", "password");
        AuthResponseDto authResponseDto = new AuthResponseDto("jwt-token", "refresh-token", 1L);

        when(authService.login(any(AuthRequestDto.class))).thenReturn(authResponseDto);

        // Perform MockMvc request without CSRF (since it's disabled in WebSecurityConfig)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andDo(print());

        // Verify that the authService.login method was called once
        verify(authService, times(1)).login(any(AuthRequestDto.class));
    }

    @Test
    void testLogout_Success() throws Exception {
        // Mock valid token
        String token = SecurityConstants.BEARER_PREFIX + "jwt-token";

        // Mock behavior for token invalidation
        doNothing().when(tokenService).invalidateToken(anyString());

        // Perform MockMvc request
        mockMvc.perform(post("/api/auth/logout")
                        .header(SecurityConstants.AUTHORIZATION_HEADER, token))
                .andExpect(status().isOk())
                .andDo(print());

        // Verify that tokenService.invalidateToken was called with the correct token
        verify(tokenService, times(1)).invalidateToken("jwt-token");
    }

    @Test
    void testLogout_InvalidToken() throws Exception {
        // Mock invalid token (without Bearer prefix)
        String token = "jwt-token";

        // Perform MockMvc request
        mockMvc.perform(post("/api/auth/logout")
                        .header(SecurityConstants.AUTHORIZATION_HEADER, token))
                .andExpect(status().isBadRequest())
                .andDo(print());

        // Verify that tokenService.invalidateToken was never called
        verify(tokenService, never()).invalidateToken(anyString());
    }

    @Test
    void testValidateToken_Valid() throws Exception {
        // Mock valid token and userId
        String token = SecurityConstants.BEARER_PREFIX + "valid-jwt-token";
        Long userId = 1L;

        // Mock behavior for token validation
        when(tokenService.validateToken(anyString(), anyLong())).thenReturn(true);

        // Perform MockMvc request
        mockMvc.perform(post("/api/auth/validate")
                        .header(SecurityConstants.AUTHORIZATION_HEADER, token)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true))
                .andDo(print());

        // Verify that tokenService.validateToken was called with the correct parameters
        verify(tokenService, times(1)).validateToken("valid-jwt-token", userId);
    }

    @Test
    void testValidateToken_Invalid() throws Exception {
        // Mock invalid token and userId
        String token = SecurityConstants.BEARER_PREFIX + "invalid-jwt-token";
        Long userId = 1L;

        // Mock behavior for token validation
        when(tokenService.validateToken(anyString(), anyLong())).thenReturn(false);

        // Perform MockMvc request
        mockMvc.perform(post("/api/auth/validate")
                        .header(SecurityConstants.AUTHORIZATION_HEADER, token)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value(false))
                .andDo(print());

        // Verify that tokenService.validateToken was called with the correct parameters
        verify(tokenService, times(1)).validateToken("invalid-jwt-token", userId);
    }
}
