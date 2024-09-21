package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.service.TransactionRedisService;
import com.jack.common.constants.TransactionConstants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TransactionRedisServiceImpl implements TransactionRedisService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionRedisServiceImpl.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Use constant with fallback to config property
    @Value("${app.transaction.cache-prefix:" + TransactionConstants.TRANSACTION_CACHE_PREFIX + "}")
    private String cachePrefix;

    @Value("${app.transaction.cache-ttl:" + TransactionConstants.TRANSACTION_CACHE_TTL + "}")
    private long cacheTTL;

    @Override
    public void saveTransactionToRedis(TransactionDto transactionDto) {
        try {
            String redisKey = cachePrefix + transactionDto.getId();
            String transactionJson = objectMapper.writeValueAsString(transactionDto);
            redisTemplate.opsForValue().set(redisKey, transactionJson, cacheTTL, TimeUnit.MINUTES);
            logger.info("Transaction with ID {} has been cached in Redis with key: {} and TTL: {} minutes", transactionDto.getId(), redisKey, cacheTTL);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize TransactionDto with ID {} for caching", transactionDto.getId(), e);
            throw new IllegalStateException("Failed to serialize TransactionDto to JSON for caching", e);
        }
    }

    @Override
    public TransactionDto getTransactionFromRedis(Long transactionId) {
        String redisKey = cachePrefix + transactionId;
        String transactionJson = redisTemplate.opsForValue().get(redisKey);
        logger.info("Transaction with ID {} was retrieved from Redis with key: {}", transactionId, redisKey);
        return Optional.ofNullable(transactionJson)
                .map(this::deserializeTransaction)
                .orElse(null);
    }

    @Override
    public void deleteTransactionFromRedis(Long transactionId) {
        String redisKey = cachePrefix + transactionId;
        redisTemplate.delete(redisKey);
        logger.info("Transaction with ID {} has been removed from Redis for key: {}", transactionId, redisKey);
    }

    private TransactionDto deserializeTransaction(String transactionJson) {
        try {
            return objectMapper.readValue(transactionJson, TransactionDto.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize transaction JSON for key: {}", transactionJson, e);
            throw new IllegalStateException("Failed to deserialize JSON to TransactionDto", e);
        }
    }
}
