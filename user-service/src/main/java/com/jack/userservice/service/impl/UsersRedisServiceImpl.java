package com.jack.userservice.service.impl;

import com.jack.common.constants.UserConstants;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.repository.UsersRepository;
import com.jack.userservice.service.UsersRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UsersRedisServiceImpl implements UsersRedisService {
    private final RedisTemplate<String, UsersDto> redisTemplate;
    private final UsersRepository usersRepository;
    private final UsersMapper usersMapper;

    @Override
    public void saveUserToRedis(UsersDto userDto) {
        Objects.requireNonNull(userDto, "User must not be null");
        Objects.requireNonNull(userDto.getId(), "User ID must not be null");
        String cacheKey = UserConstants.USER_CACHE_PREFIX + userDto.getId();
        // Cache only the UsersDto in Redis
        redisTemplate.opsForValue().set(cacheKey, userDto);
    }

    @Override
    public UsersDto getUserFromRedis(Long userId) throws Exception {
        Objects.requireNonNull(userId, "User ID must not be null");
        String cacheKey = UserConstants.USER_CACHE_PREFIX + userId;

        // Check Redis cache first
        UsersDto cachedUser = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUser != null) {
            return cachedUser;
        }

        // If not in cache, fetch from the database and convert to DTO
        Users userEntity = usersRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with ID: " + userId));
        UsersDto userDto = usersMapper.toDto(userEntity);

        // Cache the UsersDto for future requests
        redisTemplate.opsForValue().set(cacheKey, userDto);

        return userDto;
    }

    @Override
    public void deleteUserFromRedis(Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        String cacheKey = UserConstants.USER_CACHE_PREFIX + userId;

        // Invalidate cache
        redisTemplate.delete(cacheKey);
    }
}
