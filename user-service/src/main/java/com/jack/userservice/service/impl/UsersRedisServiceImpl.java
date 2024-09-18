package com.jack.userservice.service.impl;

import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.service.UsersRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UsersRedisServiceImpl implements UsersRedisService {

    private static final String USER_CACHE_PREFIX = "users:";

    private final RedisTemplate<String, UsersDto> redisTemplate;

    @Autowired
    public UsersRedisServiceImpl(RedisTemplate<String, UsersDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveUserToRedis(UsersDto user) {
        String cacheKey = USER_CACHE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user);
    }

    @Override
    public UsersDto getUserFromRedis(Long userId) {
        String cacheKey = USER_CACHE_PREFIX + userId;
        return redisTemplate.opsForValue().get(cacheKey);
    }

    @Override
    public void deleteUserFromRedis(Long userId) {
        String cacheKey = USER_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}
