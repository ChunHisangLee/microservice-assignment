package com.jack.transactionservice.service;

import com.jack.transactionservice.dto.TransactionDto;

public interface TransactionRedisService {
    void saveTransactionToRedis(TransactionDto transactionDto);

    TransactionDto getTransactionFromRedis(Long transactionId);

    void deleteTransactionFromRedis(Long transactionId);
}
