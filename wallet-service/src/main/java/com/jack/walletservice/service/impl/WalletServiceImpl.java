package com.jack.walletservice.service.impl;

import com.jack.walletservice.dto.WalletBalanceDTO;
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

@Service
public class WalletServiceImpl implements WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final RedisTemplate<String, WalletBalanceDTO> redisTemplate;
    private final WalletBalancePublisher walletBalancePublisher;

    @Value("${app.wallet.cache-prefix}")
    private String cachePrefix;

    public WalletServiceImpl(WalletRepository walletRepository, RedisTemplate<String, WalletBalanceDTO> redisTemplate, WalletBalancePublisher walletBalancePublisher) {
        this.walletRepository = walletRepository;
        this.redisTemplate = redisTemplate;
        this.walletBalancePublisher = walletBalancePublisher;
    }

    @Transactional
    @Override
    public Wallet createWallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setUsdBalance(0.0); // Default USD balance
        wallet.setBtcBalance(0.0); // Default BTC balance
        wallet = walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet created and balance published for user ID: {}", userId);
        return wallet;
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
    public void updateWallet(Long userId, Double usdAmount, Double btcAmount) {
        Wallet wallet = getWalletByUserId(userId);
        logger.info("Updating wallet for user ID: {} | USD: {} | BTC: {}", userId, usdAmount, btcAmount);
        wallet.setUsdBalance(wallet.getUsdBalance() + usdAmount);
        wallet.setBtcBalance(wallet.getBtcBalance() + btcAmount);
        walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet updated and balance published for user ID: {}", userId);
    }

    @Transactional
    @Override
    public void creditWallet(Long userId, Double amount) {
        Wallet wallet = getWalletByUserId(userId);
        logger.info("Crediting {} USD to wallet of user ID: {}", amount, userId);
        wallet.setUsdBalance(wallet.getUsdBalance() + amount);
        walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet credited and balance published for user ID: {}", userId);
    }

    @Transactional
    @Override
    public void debitWallet(Long userId, Double amount) {
        Wallet wallet = getWalletByUserId(userId);
        logger.info("Debiting {} USD from wallet of user ID: {}", amount, userId);

        if (wallet.getUsdBalance() < amount) {
            logger.error("Insufficient balance. Attempted to debit {} USD from user ID: {}", amount, userId);
            throw new InsufficientFundsException("Insufficient USD balance.");
        }

        wallet.setUsdBalance(wallet.getUsdBalance() - amount);
        walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet debited and balance published for user ID: {}", userId);
    }

    @Override
    public WalletBalanceDTO getWalletBalance(Long userId) {
        String cacheKey = cachePrefix + userId;

        // Check Redis cache first
        WalletBalanceDTO cachedBalance = redisTemplate.opsForValue().get(cacheKey);
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

    @Override
    public void updateWalletBalance(WalletBalanceDTO walletBalanceDTO) {
        Wallet wallet = getWalletByUserId(walletBalanceDTO.getUserId());
        wallet.setUsdBalance(walletBalanceDTO.getUsdBalance());
        wallet.setBtcBalance(walletBalanceDTO.getBtcBalance());
        walletRepository.save(wallet);

        updateCacheAndNotify(wallet);

        logger.info("Wallet and cache updated, balance published for user ID: {}", walletBalanceDTO.getUserId());
    }

    private WalletBalanceDTO updateCacheAndNotify(Wallet wallet) {
        WalletBalanceDTO walletBalanceDTO = WalletBalanceDTO.builder()
                .userId(wallet.getUserId())
                .usdBalance(wallet.getUsdBalance())
                .btcBalance(wallet.getBtcBalance())
                .build();

        String cacheKey = cachePrefix + wallet.getUserId();

        // Update Redis cache
        redisTemplate.opsForValue().set(cacheKey, walletBalanceDTO);
        logger.info("Cache updated for user ID: {}", wallet.getUserId());

        // Notify other services via RabbitMQ
        walletBalancePublisher.publishWalletBalance(walletBalanceDTO);
        logger.info("Balance published for user ID: {}", wallet.getUserId());

        return walletBalanceDTO;
    }
}
