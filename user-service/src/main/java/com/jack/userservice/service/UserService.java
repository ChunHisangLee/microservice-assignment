package com.jack.userservice.service;

import com.jack.common.dto.request.UserRegistrationRequestDto;
import com.jack.common.dto.response.UserResponseDto;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;

import java.util.Optional;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto userRegistrationRequestDto);

    Optional<Users> updateUser(Long id, Users users);

    void deleteUser(Long id);

    boolean verifyPassword(String email, String rawPassword);

    UsersDto getUserWithBalance(Long userId);
}
