package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.ApplicationConstants;
import com.jack.common.constants.TransactionConstants;
import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.transactionservice.client.OutboxClient;
import com.jack.transactionservice.client.WalletServiceClient;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.mapper.TransactionMapper;
import com.jack.transactionservice.repository.TransactionRepository;
import com.jack.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String btcPriceKey = ApplicationConstants.BTC_PRICE_KEY;

    @Override
    public TransactionDto createTransaction(CreateTransactionRequestDto request, TransactionType transactionType) {
        // Step 1: Fetch BTC price and btcPriceHistoryId from Redis
        BTCPriceResponseDto btcPrice = getCurrentBTCPriceFromRedis();

        // Step 2: Create and save the transaction in the database
        Transaction transaction = Transaction.builder()
                .userId(request.getUserId())
                .btcAmount(request.getBtcAmount())
                .usdAmount(calculateUsdAmount(btcPrice.getBtcPrice(), request.getBtcAmount()))
                .btcPriceHistoryId(btcPrice.getId())
                .transactionType(transactionType)
                .transactionTime(LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);

        // Step 3: Publish the event to the outbox service
        outboxClient.sendTransactionEvent(transaction.getId(), request.getUserId(), transaction.getBtcAmount(), transaction.getUsdAmount());

        // Step 4: Calculate the updated wallet balances
        BigDecimal newUsdBalance = calculateNewUsdBalance(request.getUsdBalanceBefore(), transaction.getBtcAmount(), transactionType);
        BigDecimal newBtcBalance = calculateNewBtcBalance(request.getBtcBalanceBefore(), transaction.getBtcAmount(), transactionType);

        // Step 5: Call the WalletService to update the user's wallet balances
        walletServiceClient.updateWalletBalance(request.getUserId(), newUsdBalance.doubleValue(), newBtcBalance.doubleValue());

        // Step 6: Cache the transaction data in Redis
        cacheTransaction(transaction);

        // Step 7: Return the transaction DTO
        return transactionMapper.toDto(transaction, request.getUsdBalanceBefore(), request.getBtcBalanceBefore(), newUsdBalance, newBtcBalance);
    }

    @Override
    public Page<TransactionDto> getUserTransactionHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(transaction -> transactionMapper.toDto(transaction, BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)));
    }

    private void cacheTransaction(Transaction transaction) {
        String redisKey = TransactionConstants.TRANSACTION_CACHE_PREFIX + transaction.getId();
        try {
            // Serialize a transaction object to JSON for caching
            String transactionJson = objectMapper.writeValueAsString(transaction);
            redisTemplate.opsForValue().set(redisKey, transactionJson, TransactionConstants.TRANSACTION_CACHE_TTL, TimeUnit.MINUTES);  // Use constant TTL
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Transaction to cache: " + e.getMessage(), e);
        }
    }

    private BigDecimal calculateNewUsdBalance(BigDecimal usdBalanceBefore, BigDecimal btcAmount, TransactionType transactionType) {
        BigDecimal btcPrice = BigDecimal.valueOf(getCurrentBtcPrice());  // Fetch BTC price from Redis
        if (transactionType == TransactionType.BUY) {
            return usdBalanceBefore.subtract(btcAmount.multiply(btcPrice));
        } else {
            return usdBalanceBefore.add(btcAmount.multiply(btcPrice));
        }
    }

    private BigDecimal calculateNewBtcBalance(BigDecimal btcBalanceBefore, BigDecimal btcAmount, TransactionType transactionType) {
        if (transactionType == TransactionType.BUY) {
            return btcBalanceBefore.add(btcAmount);
        } else {
            return btcBalanceBefore.subtract(btcAmount);
        }
    }

    private BTCPriceResponseDto getCurrentBTCPriceFromRedis() {
        String btcPriceStr = redisTemplate.opsForValue().get(btcPriceKey);
        return Optional.ofNullable(btcPriceStr)
                .map(this::parseBTCPriceResponse)
                .orElseThrow(() -> new IllegalStateException("BTC price not found in Redis"));
    }

    private BTCPriceResponseDto parseBTCPriceResponse(String btcPriceStr) {
        try {
            return objectMapper.readValue(btcPriceStr, BTCPriceResponseDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse BTC price from Redis: " + e.getMessage(), e);
        }
    }

    private double getCurrentBtcPrice() {
        String priceStr = redisTemplate.opsForValue().get(btcPriceKey);
        return Optional.ofNullable(priceStr)
                .map(this::parsePrice)
                .orElseThrow(() -> new IllegalStateException("BTC price not found in Redis"));
    }

    private double parsePrice(String priceStr) {
        try {
            return Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Failed to parse BTC price from Redis: " + e.getMessage(), e);
        }
    }

    private BigDecimal calculateUsdAmount(BigDecimal btcPrice, BigDecimal btcAmount) {
        return btcPrice.multiply(btcAmount);
    }
}
