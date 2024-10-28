package com.jack.userservice.config;

import com.jack.common.config.RedisCommonConfig;
import com.jack.userservice.dto.UsersDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Import(RedisCommonConfig.class)
@Log4j2
public class RedisConfig {

    @Bean("redisTemplateForUsers")
    public RedisTemplate<String, UsersDto> redisTemplateForUsers(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, UsersDto> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Set up key and value serializers
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        log.info("Configured redisTemplateForUsers with StringRedisSerializer for keys and GenericJackson2JsonRedisSerializer for values.");
        return redisTemplate;
    }
}
