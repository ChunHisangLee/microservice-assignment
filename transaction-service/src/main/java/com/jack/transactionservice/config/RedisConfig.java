package com.jack.transactionservice.config;

import com.jack.common.config.RedisCommonConfig;
import com.jack.transactionservice.dto.TransactionDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Import(RedisCommonConfig.class)
@Log4j2
public class RedisConfig {

    @Bean("redisTransactionTemplate")
    public RedisTemplate<String, TransactionDto> redisTransactionTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, TransactionDto> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Set up key and value serializers
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        log.info("Configured redisTransactionTemplate with StringRedisSerializer for keys and GenericJackson2JsonRedisSerializer for values.");
        return redisTemplate;
    }

    // Redis template for handling pages of TransactionDto
    @Bean("redisTransactionPageTemplate")
    public RedisTemplate<String, Page<TransactionDto>> redisTransactionPageTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Page<TransactionDto>> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Set up key and value serializers
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        log.info("Configured redisTransactionPageTemplate with StringRedisSerializer for keys and GenericJackson2JsonRedisSerializer for values.");
        return redisTemplate;
    }
}
