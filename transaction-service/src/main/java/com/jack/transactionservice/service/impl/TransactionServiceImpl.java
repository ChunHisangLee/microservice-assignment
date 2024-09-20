package com.jack.transactionservice.service.impl;

import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.transactionservice.client.OutboxClient;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.mapper.TransactionMapper;
import com.jack.transactionservice.repository.TransactionRepository;
import com.jack.transactionservice.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final OutboxClient outboxClient;
    private final RedisTemplate<String, Page<TransactionDto>> redisTransactionPageTemplate; // Typed RedisTemplate

    @Value("${app.transaction.cache-prefix}")
    private String cachePrefix;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  TransactionMapper transactionMapper,
                                  OutboxClient outboxClient,
                                  RedisTemplate<String, Page<TransactionDto>> redisTransactionPageTemplate) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.outboxClient = outboxClient;
        this.redisTransactionPageTemplate = redisTransactionPageTemplate;
    }

    @Override
    public TransactionDto createTransaction(CreateTransactionRequestDto request, TransactionType transactionType) {
        // Create the transaction and save it
        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setBtcAmount(request.getBtcAmount());
        transaction.setTransactionType(transactionType);
        transaction = transactionRepository.save(transaction);

        // Store transaction in Redis with expiration (e.g., 10 minutes)
        String redisKey = cachePrefix + transaction.getId();
        redisTransactionPageTemplate.opsForValue().set(redisKey, Page.empty(), 10, TimeUnit.MINUTES); // Storing empty page for simplicity

        // Send the event to the outbox service
        outboxClient.sendTransactionEvent(transaction.getId(), request.getUserId(), transaction.getBtcAmount());

        return transactionMapper.toDto(transaction, request.getUsdBalanceBefore(), request.getBtcBalanceBefore(),
                request.getUsdBalanceAfter(), request.getBtcBalanceAfter());
    }

    @Override
    public Page<TransactionDto> getUserTransactionHistory(Long userId, Pageable pageable) {
        // Attempt to retrieve from Redis cache first
        String redisKey = cachePrefix + "user:" + userId;
        Page<TransactionDto> cachedTransactions = redisTransactionPageTemplate.opsForValue().get(redisKey);

        if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
            return cachedTransactions; // Return cached transactions if available
        }

        // Fetch from the database if cache misses
        Page<TransactionDto> transactions = transactionRepository.findByUserId(userId, pageable)
                .map(transaction -> transactionMapper.toDto(transaction, 0.0, 0.0, 0.0, 0.0));

        // Cache the result in Redis with expiration (e.g., 10 minutes)
        redisTransactionPageTemplate.opsForValue().set(redisKey, transactions, 10, TimeUnit.MINUTES);

        return transactions;
    }
}
