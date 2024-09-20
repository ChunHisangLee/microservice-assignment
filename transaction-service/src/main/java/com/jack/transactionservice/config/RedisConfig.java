package com.jack.transactionservice.config;

import com.jack.transactionservice.dto.TransactionDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // Redis template for handling individual TransactionDto
    @Bean
    public RedisTemplate<String, TransactionDto> redisTransactionTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, TransactionDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer()); // JSON serialization
        return template;
    }

    // Redis template for handling pages of TransactionDto
    @Bean
    public RedisTemplate<String, Page<TransactionDto>> redisTransactionPageTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Page<TransactionDto>> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer()); // JSON serialization
        return template;
    }
}
