package com.jack.userservice.service;

import com.jack.userservice.dto.UsersDTO;

public interface UsersRedisService {

    // Save UserDTO to Redis
    void saveUserToRedis(UsersDTO user);

    // Retrieve UserDTO from Redis
    UsersDTO getUserFromRedis(Long userId);

    // Delete UserDTO from Redis
    void deleteUserFromRedis(Long userId);
}
