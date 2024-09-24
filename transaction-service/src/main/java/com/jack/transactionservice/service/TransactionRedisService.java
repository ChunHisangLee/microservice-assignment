package com.jack.transactionservice.service;

import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.transactionservice.dto.TransactionDto;

public interface TransactionRedisService {
    void saveTransactionToRedis(TransactionDto transactionDto);

    TransactionDto getTransactionFromRedis(Long transactionId);

    String getBTCPriceFromRedis(String btcPriceKey);
}
