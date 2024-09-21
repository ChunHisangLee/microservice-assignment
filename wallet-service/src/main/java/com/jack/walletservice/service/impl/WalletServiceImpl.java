package com.jack.walletservice.service.impl;

import com.jack.common.dto.response.WalletBalanceDto;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.walletservice.entity.Wallet;
import com.jack.walletservice.exception.InsufficientFundsException;
import com.jack.walletservice.exception.WalletNotFoundException;
import com.jack.walletservice.publisher.WalletBalancePublisher;
import com.jack.walletservice.repository.WalletRepository;
import com.jack.walletservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final RedisTemplate<String, WalletBalanceDto> redisTemplate;
    private final WalletBalancePublisher walletBalancePublisher;

    @Value("${app.wallet.cache-prefix}")
    private String cachePrefix;

    public WalletServiceImpl(WalletRepository walletRepository, RedisTemplate<String, WalletBalanceDto> redisTemplate, WalletBalancePublisher walletBalancePublisher) {
        this.walletRepository = walletRepository;
        this.redisTemplate = redisTemplate;
        this.walletBalancePublisher = walletBalancePublisher;
    }

    @Transactional
    @Override
    public void createWallet(WalletCreateMessageDto message) {
        // Check if wallet already exists for the user
        if (walletExists(message.getUserId())) {
            logger.warn("Wallet for user ID {} already exists. Skipping wallet creation.", message.getUserId());
            return;
        }

        // Create a wallet for the user
        Wallet wallet = new Wallet();
        wallet.setUserId(message.getUserId());
        wallet.setUsdBalance(message.getInitialBalance());
        wallet.setBtcBalance(BigDecimal.valueOf(0.0));

        wallet = walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet created successfully for user ID: {}", message.getUserId());
    }


    @Transactional(readOnly = true)
    @Override
    public Wallet getWalletByUserId(Long userId) {
        logger.info("Fetching wallet for user ID: {}", userId);
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user ID: " + userId));
    }

    @Transactional
    @Override
    public void updateWallet(Long userId, BigDecimal usdAmount, BigDecimal btcAmount) {
        Wallet wallet = getWalletByUserId(userId);
        logger.info("Updating wallet for user ID: {} | USD: {} | BTC: {}", userId, usdAmount, btcAmount);
        wallet.setUsdBalance(wallet.getUsdBalance().add(usdAmount));
        wallet.setBtcBalance(wallet.getBtcBalance().add(btcAmount));
        walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet updated and balance published for user ID: {}", userId);
    }

    @Transactional
    @Override
    public void debitWallet(Long userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);
        logger.info("Debiting {} USD from wallet of user ID: {}", amount, userId);

        if (wallet.getUsdBalance().compareTo(amount) < 0) {
            logger.error("Insufficient balance. Attempted to debit {} USD from user ID: {}", amount, userId);
            throw new InsufficientFundsException("Insufficient USD balance.");
        }

        wallet.setUsdBalance(wallet.getUsdBalance().subtract(amount));
        walletRepository.save(wallet);
        updateCacheAndNotify(wallet);
        logger.info("Wallet debited and balance published for user ID: {}", userId);
    }

    @Transactional
    @Override
    public WalletBalanceDto getWalletBalance(Long userId) {
        String cacheKey = cachePrefix + userId;

        // Check Redis cache first
        WalletBalanceDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);

        if (cachedBalance != null) {
            logger.info("Cache hit for user ID: {}", userId);
            return cachedBalance;
        }

        // If not found in cache, fetch from the database
        Wallet wallet = getWalletByUserId(userId);
        return updateCacheAndNotify(wallet);
    }

    @Override
    public boolean walletExists(Long userId) {
        return walletRepository.findByUserId(userId).isPresent();
    }

    @Transactional
    @Override
    public void updateWalletBalance(WalletBalanceDto WalletBalanceDto) {
        Wallet wallet = getWalletByUserId(WalletBalanceDto.getUserId());
        wallet.setUsdBalance(WalletBalanceDto.getUsdBalance());
        wallet.setBtcBalance(WalletBalanceDto.getBtcBalance());
        walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet and cache updated, balance published for user ID: {}", WalletBalanceDto.getUserId());
    }

    private WalletBalanceDto updateCacheAndNotify(Wallet wallet) {
        WalletBalanceDto walletBalanceDto = WalletBalanceDto.builder()
                .userId(wallet.getUserId())
                .usdBalance(wallet.getUsdBalance())
                .btcBalance(wallet.getBtcBalance())
                .build();

        String cacheKey = cachePrefix + wallet.getUserId();

        // Update Redis cache
        redisTemplate.opsForValue().set(cacheKey, walletBalanceDto);
        logger.info("Cache updated for user ID: {}", wallet.getUserId());

        // Notify other services via RabbitMQ
        walletBalancePublisher.publishWalletBalance(walletBalanceDto);
        logger.info("Balance published for user ID: {}", wallet.getUserId());

        return walletBalanceDto;
    }
}
