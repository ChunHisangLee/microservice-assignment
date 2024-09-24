package com.jack.transactionservice.service;

import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    TransactionDto createTransaction(CreateTransactionRequestDto request, TransactionType transactionType);

    Page<TransactionDto> getUserTransactionHistory(Long userId, Pageable pageable);

    TransactionDto getTransactionById(Long transactionId);
}
