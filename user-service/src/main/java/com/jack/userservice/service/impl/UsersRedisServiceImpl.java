package com.jack.userservice.service.impl;

import com.jack.common.constants.UserConstants;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.service.UsersRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UsersRedisServiceImpl implements UsersRedisService {
    private final RedisTemplate<String, UsersDto> redisTemplate;

    @Autowired
    public UsersRedisServiceImpl(RedisTemplate<String, UsersDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveUserToRedis(UsersDto user) {
        Objects.requireNonNull(user, "User must not be null");
        Objects.requireNonNull(user.getId(), "User ID must not be null");
        String cacheKey = UserConstants.USER_CACHE_PREFIX + user.getId();
        redisTemplate.opsForValue()
                .set(cacheKey, user);
    }

    @Override
    public UsersDto getUserFromRedis(Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        String cacheKey = UserConstants.USER_CACHE_PREFIX + userId;
        return redisTemplate.opsForValue()
                .get(cacheKey);
    }

    @Override
    public void deleteUserFromRedis(Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        String cacheKey = UserConstants.USER_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}
