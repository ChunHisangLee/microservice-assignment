package com.jack.transactionservice.service.impl;

import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.transactionservice.client.OutboxClient;
import com.jack.transactionservice.client.WalletServiceClient;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.mapper.TransactionMapper;
import com.jack.transactionservice.repository.TransactionRepository;
import com.jack.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final OutboxClient outboxClient;
    private final WalletServiceClient walletServiceClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.transaction.cache-prefix}")
    private String cachePrefix;

    @Value("${app.redis.btc-price-key}")
    private String btcPriceKey;

    @Override
    public TransactionDto createTransaction(CreateTransactionRequestDto request, TransactionType transactionType) {
        // Step 1: Create and save the transaction in the database
        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setBtcAmount(request.getBtcAmount());
        transaction.setTransactionType(transactionType);
        transaction = transactionRepository.save(transaction);

        // Step 2: Publish the event to the outbox service
        outboxClient.sendTransactionEvent(transaction.getId(), request.getUserId(), transaction.getBtcAmount());

        // Step 3: Calculate the updated wallet balances
        double newUsdBalance = calculateNewUsdBalance(request.getUsdBalanceBefore(), transaction.getBtcAmount(), transactionType);
        double newBtcBalance = calculateNewBtcBalance(request.getBtcBalanceBefore(), transaction.getBtcAmount(), transactionType);

        // Step 4: Call the WalletService to update the user's wallet balances
        walletServiceClient.updateWalletBalance(request.getUserId(), newUsdBalance, newBtcBalance);

        // Step 5: Cache the transaction data in Redis using cachePrefix
        cacheTransaction(transaction);

        // Step 6: Return the transaction DTO
        return transactionMapper.toDto(transaction, request.getUsdBalanceBefore(), request.getBtcBalanceBefore(),
                request.getUsdBalanceAfter(), request.getBtcBalanceAfter());
    }

    @Override
    public Page<TransactionDto> getUserTransactionHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(transaction -> transactionMapper.toDto(transaction, 0.0, 0.0, 0.0, 0.0));
    }

    // Cache the transaction in Redis using cachePrefix
    private void cacheTransaction(Transaction transaction) {
        String redisKey = cachePrefix + "transaction:" + transaction.getId();
        redisTemplate.opsForValue().set(redisKey, transaction.toString(), 10, TimeUnit.MINUTES); // Example: caching for 10 minutes
    }

    // Helper method to calculate the new USD balance based on the transaction type
    private double calculateNewUsdBalance(double usdBalanceBefore, double btcAmount, TransactionType transactionType) {
        double btcPrice = getCurrentBtcPrice(); // Fetch BTC price from Redis
        if (transactionType == TransactionType.BUY) {
            return usdBalanceBefore - (btcAmount * btcPrice);
        } else {
            return usdBalanceBefore + (btcAmount * btcPrice);
        }
    }

    // Helper method to calculate the new BTC balance based on the transaction type
    private double calculateNewBtcBalance(double btcBalanceBefore, double btcAmount, TransactionType transactionType) {
        if (transactionType == TransactionType.BUY) {
            return btcBalanceBefore + btcAmount;
        } else {
            return btcBalanceBefore - btcAmount;
        }
    }

    // Fetch the current BTC price from Redis
    private double getCurrentBtcPrice() {
        String priceStr = redisTemplate.opsForValue().get(btcPriceKey);
        return Optional.ofNullable(priceStr)
                .map(this::parsePrice)
                .orElseThrow(() -> new IllegalStateException("BTC price not found in Redis"));
    }

    // Helper method to parse the BTC price from a string
    private double parsePrice(String priceStr) {
        try {
            return Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Failed to parse BTC price from Redis", e);
        }
    }
}
