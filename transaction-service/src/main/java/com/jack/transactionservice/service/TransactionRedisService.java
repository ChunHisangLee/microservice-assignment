package com.jack.transactionservice.service;

import com.jack.transactionservice.dto.TransactionDto;

public interface TransactionRedisService {

    // Save TransactionDto to Redis
    void saveTransactionToRedis(TransactionDto transactionDto);

    // Retrieve TransactionDto from Redis
    TransactionDto getTransactionFromRedis(Long transactionId);

    // Delete TransactionDto from Redis
    void deleteTransactionFromRedis(Long transactionId);
}
