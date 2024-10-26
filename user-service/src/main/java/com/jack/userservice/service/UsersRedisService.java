package com.jack.userservice.service;

import com.jack.userservice.dto.UsersDto;

public interface UsersRedisService {

    // Save UserDTO to Redis
    void saveUserToRedis(UsersDto user);

    // Retrieve UserDTO from Redis
    UsersDto getUserFromRedis(Long userId) throws Exception;

    // Delete UserDTO from Redis
    void deleteUserFromRedis(Long userId);
}
