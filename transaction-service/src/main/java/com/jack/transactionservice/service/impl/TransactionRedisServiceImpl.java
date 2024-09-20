package com.jack.transactionservice.service.impl;

import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.service.TransactionRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TransactionRedisServiceImpl implements TransactionRedisService {

    private final RedisTemplate<String, TransactionDto> redisTransactionTemplate;
    private final RedisTemplate<String, Page<TransactionDto>> redisTransactionPageTemplate;
    @Value("${app.transaction.cache-prefix}")
    private String cachePrefix;

    @Autowired
    public TransactionRedisServiceImpl(RedisTemplate<String, TransactionDto> redisTransactionTemplate,
                                       RedisTemplate<String, Page<TransactionDto>> redisTransactionPageTemplate) {
        this.redisTransactionTemplate = redisTransactionTemplate;
        this.redisTransactionPageTemplate = redisTransactionPageTemplate;
    }

    @Override
    public void saveTransactionToRedis(TransactionDto transactionDto) {
        String redisKey = cachePrefix + transactionDto.getId();
        redisTransactionTemplate.opsForValue().set(redisKey, transactionDto, 10, TimeUnit.MINUTES); // Cache with 10 min expiration
    }

    @Override
    public TransactionDto getTransactionFromRedis(Long transactionId) {
        String redisKey = cachePrefix + transactionId;
        return redisTransactionTemplate.opsForValue().get(redisKey);
    }

    @Override
    public void deleteTransactionFromRedis(Long transactionId) {
        String redisKey = cachePrefix + transactionId;
        redisTransactionTemplate.delete(redisKey);
    }

    public void saveTransactionPageToRedis(Page<TransactionDto> transactionPage, Long userId) {
        String redisKey = cachePrefix + "user:" + userId;
        redisTransactionPageTemplate.opsForValue().set(redisKey, transactionPage, 10, TimeUnit.MINUTES);
    }

    public Page<TransactionDto> getTransactionPageFromRedis(Long userId) {
        String redisKey = cachePrefix + "user:" + userId;
        return redisTransactionPageTemplate.opsForValue().get(redisKey);
    }

    public void deleteTransactionPageFromRedis(Long userId) {
        String redisKey = cachePrefix + "user:" + userId;
        redisTransactionPageTemplate.delete(redisKey);
    }
}
