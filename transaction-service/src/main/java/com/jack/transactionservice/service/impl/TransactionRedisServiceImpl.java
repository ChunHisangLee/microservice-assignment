package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.TransactionConstants;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.service.TransactionRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionRedisServiceImpl implements TransactionRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

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
            log.info("Transaction with ID {} has been cached in Redis with key: {} and TTL: {} minutes", transactionDto.getId(), redisKey, cacheTTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TransactionDto with ID {} for caching", transactionDto.getId(), e);
            throw new IllegalStateException("Failed to serialize TransactionDto to JSON for caching", e);
        }
    }

    @Override
    public TransactionDto getTransactionFromRedis(Long transactionId) {
        String redisKey = cachePrefix + transactionId;
        String transactionJson = redisTemplate.opsForValue().get(redisKey);
        log.info("Transaction with ID {} was retrieved from Redis with key: {}", transactionId, redisKey);
        return Optional.ofNullable(transactionJson)
                .map(this::deserializeTransaction)
                .orElse(null);
    }

    @Override
    public String getBTCPriceFromRedis(String btcPriceKey) {
        String btcPriceStr = redisTemplate.opsForValue().get(btcPriceKey);
        log.info("Fetching BTC price from Redis with key: {}", btcPriceKey);
        return btcPriceStr;
    }

    private TransactionDto deserializeTransaction(String transactionJson) {
        try {
            return objectMapper.readValue(transactionJson, TransactionDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize transaction JSON for key: {}", transactionJson, e);
            throw new IllegalStateException("Failed to deserialize JSON to TransactionDto", e);
        }
    }
}
