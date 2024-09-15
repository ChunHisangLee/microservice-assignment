package com.jack.userservice.service;

import com.jack.common.dto.UserRegistrationDTO;
import com.jack.common.dto.UserResponseDTO;
import com.jack.userservice.dto.UsersDTO;
import com.jack.userservice.entity.Users;

import java.util.Optional;

public interface UserService {
    UserResponseDTO register(UserRegistrationDTO registrationDTO);

    Optional<Users> updateUser(Long id, Users users);

    void deleteUser(Long id);

    Users login(String email, String password);

    Optional<Users> getUserById(Long id);

    Optional<Users> findByEmail(String email);

    boolean verifyPassword(String email, String rawPassword);

    UsersDTO getUserWithBalance(Long userId);
}
