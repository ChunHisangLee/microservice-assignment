package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.ApplicationConstants;
import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.transactionservice.client.OutboxClient;
import com.jack.transactionservice.client.WalletServiceClient;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.mapper.TransactionMapper;
import com.jack.transactionservice.repository.TransactionRepository;
import com.jack.transactionservice.service.TransactionRedisService;
import com.jack.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final OutboxClient outboxClient;
    private final WalletServiceClient walletServiceClient;
    private final TransactionRedisService transactionRedisService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TransactionDto createTransaction(CreateTransactionRequestDto request, TransactionType transactionType) {
        log.info("Creating transaction for user: {}", request.getUserId());

        // Step 1: Fetch BTC price and btcPriceHistoryId from Redis
        BTCPriceResponseDto btcPrice = getCurrentBTCPriceFromRedis();

        // Step 2: Fetch user's current wallet balances from WalletService
        WalletResponseDto currentBalances = walletServiceClient.getWalletBalance(request.getUserId());
        log.info("Fetched wallet balances for user {}: USD - {}, BTC - {}", request.getUserId(),
                currentBalances.getUsdBalance(), currentBalances.getBtcBalance());

        // Step 3: Calculate USD amount based on transaction type
        BigDecimal usdAmount = calculateUsdAmount(btcPrice.getBtcPrice(), request.getBtcAmount(), transactionType);

        // Step 4: Calculate the updated wallet balances
        BigDecimal newUsdBalance = calculateNewUsdBalance(currentBalances.getUsdBalance(), request.getBtcAmount(),
                transactionType, btcPrice.getBtcPrice());
        BigDecimal newBtcBalance = calculateNewBtcBalance(currentBalances.getBtcBalance(), request.getBtcAmount(),
                transactionType);

        // Step 5: Validate the new balances
        validateBalances(newUsdBalance, newBtcBalance);

        // Step 6: Create and save the transaction in the database
        Transaction transaction = Transaction.builder()
                .userId(request.getUserId())
                .btcAmount(request.getBtcAmount())
                .usdAmount(usdAmount)
                .btcPriceHistoryId(btcPrice.getId())
                .transactionType(transactionType)
                .transactionTime(LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);

        log.info("Transaction created with ID: {}", transaction.getId());

        // Step 7: Publish the event to the outbox service
        outboxClient.sendTransactionEvent(transaction.getId(), request.getUserId(), transaction.getBtcAmount(),
                transaction.getUsdAmount());

        // Step 8: Call the WalletService to update the user's wallet balances
        walletServiceClient.updateWalletBalance(request.getUserId(), newUsdBalance, newBtcBalance);

        // Step 9: Cache the transaction data in Redis via TransactionRedisService
        cacheTransaction(transaction);

        // Step 10: Return the transaction response DTO
        return transactionMapper.toDto(transaction, currentBalances.getUsdBalance(),
                currentBalances.getBtcBalance(), newUsdBalance, newBtcBalance);
    }

    @Override
    public Page<TransactionDto> getUserTransactionHistory(Long userId, Pageable pageable) {
        log.info("Fetching transaction history for user: {}", userId);
        return transactionRepository.findByUserId(userId, pageable)
                .map(transaction -> transactionMapper.toDto(transaction, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Override
    public TransactionDto getTransactionById(Long transactionId) {
        log.info("Retrieving transaction with ID: {}", transactionId);
        // Attempt to retrieve from cache first
        TransactionDto cachedTransaction = transactionRedisService.getTransactionFromRedis(transactionId);
        if (cachedTransaction != null) {
            log.info("Transaction with ID {} retrieved from cache", transactionId);
            return cachedTransaction;
        }

        // If not in cache, retrieve from database
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found with ID: " + transactionId));

        // Map to DTO
        TransactionDto transactionDto = transactionMapper.toDto(transaction, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // Cache the retrieved transaction for future requests
        transactionRedisService.saveTransactionToRedis(transactionDto);
        log.info("Transaction with ID {} has been cached after retrieval from DB", transactionId);

        return transactionDto;
    }

    private void cacheTransaction(Transaction transaction) {
        TransactionDto transactionDto = transactionMapper.toDto(transaction, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        transactionRedisService.saveTransactionToRedis(transactionDto);
        log.info("Transaction with ID {} has been cached successfully.", transaction.getId());
    }

    private BigDecimal calculateNewUsdBalance(BigDecimal usdBalanceBefore, BigDecimal btcAmount, TransactionType transactionType, BigDecimal btcPrice) {
        BigDecimal usdChange = btcPrice.multiply(btcAmount);

        if (transactionType == TransactionType.BUY) {
            return usdBalanceBefore.subtract(usdChange);
        } else if (transactionType == TransactionType.SELL) {
            return usdBalanceBefore.add(usdChange);
        } else {
            throw new IllegalArgumentException("Unsupported Transaction Type: " + transactionType);
        }
    }

    private BigDecimal calculateNewBtcBalance(BigDecimal btcBalanceBefore, BigDecimal btcAmount, TransactionType transactionType) {
        if (transactionType == TransactionType.BUY) {
            return btcBalanceBefore.add(btcAmount);
        } else if (transactionType == TransactionType.SELL) {
            return btcBalanceBefore.subtract(btcAmount);
        } else {
            throw new IllegalArgumentException("Unsupported Transaction Type: " + transactionType);
        }
    }

    private void validateBalances(BigDecimal usdBalanceAfter, BigDecimal btcBalanceAfter) {
        if (usdBalanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient USD balance for this transaction.");
        }

        if (btcBalanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient BTC balance for this transaction.");
        }
    }

    private BTCPriceResponseDto getCurrentBTCPriceFromRedis() {
        String btcPriceKey = ApplicationConstants.BTC_PRICE_KEY;
        log.info("Fetching BTC price from Redis with key: {}", btcPriceKey);
        String btcPriceStr = transactionRedisService.getBTCPriceFromRedis(btcPriceKey);
        return Optional.ofNullable(btcPriceStr)
                .map(this::parseBTCPriceResponse)
                .orElseThrow(() -> new IllegalStateException("BTC price not found in Redis"));
    }

    private BTCPriceResponseDto parseBTCPriceResponse(String btcPriceStr) {
        try {
            log.info("Parsing BTC price response string: {}", btcPriceStr);
            return objectMapper.readValue(btcPriceStr, BTCPriceResponseDto.class);
        } catch (Exception e) {
            log.error("Error deserializing BTCPriceResponseDto from JSON: {}", e.getMessage());
            throw new IllegalStateException("Failed to parse BTC price from Redis: " + e.getMessage(), e);
        }
    }

    private BigDecimal calculateUsdAmount(BigDecimal btcPrice, BigDecimal btcAmount, TransactionType transactionType) {
        BigDecimal amount = btcPrice.multiply(btcAmount);

        if (transactionType == TransactionType.BUY) {
            return amount;
        } else if (transactionType == TransactionType.SELL) {
            return amount.negate(); // Selling BTC adds USD
        } else {
            throw new IllegalArgumentException("Unsupported Transaction Type: " + transactionType);
        }
    }
}
