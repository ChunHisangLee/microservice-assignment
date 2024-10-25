package com.jack.userservice.service;

import com.jack.common.dto.request.UserRegistrationRequestDto;
import com.jack.userservice.dto.UserResponseDto;
import com.jack.userservice.dto.UserUpdateRequestDto;
import com.jack.userservice.dto.UsersDto;

import java.util.Optional;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto userRegistrationRequestDto);

    Optional<UserResponseDto> updateUser(Long id, UserUpdateRequestDto userUpdateRequestDto);

    void deleteUser(Long id);

    boolean isPasswordValid(String email, String rawPassword);

    Optional<UsersDto> getUserWithBalance(Long userId);
}
