package com.jack.walletservice.service.impl;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.walletservice.entity.Wallet;
import com.jack.walletservice.exception.InsufficientFundsException;
import com.jack.walletservice.exception.WalletNotFoundException;
import com.jack.walletservice.publisher.WalletBalancePublisher;
import com.jack.walletservice.repository.WalletRepository;
import com.jack.walletservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final RedisTemplate<String, WalletResponseDto> redisTemplate;
    private final WalletBalancePublisher walletBalancePublisher;
    private final String cachePrefix = WalletConstants.WALLET_CACHE_PREFIX;

    public WalletServiceImpl(WalletRepository walletRepository, RedisTemplate<String, WalletResponseDto> redisTemplate, WalletBalancePublisher walletBalancePublisher) {
        this.walletRepository = walletRepository;
        this.redisTemplate = redisTemplate;
        this.walletBalancePublisher = walletBalancePublisher;
    }

    @Transactional
    @Override
    public void createWallet(WalletCreateMessageDto message) {
        if (walletExists(message.getUserId())) {
            logger.warn("Wallet for user ID {} already exists. Skipping wallet creation.", message.getUserId());
            return;
        }

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
    public WalletResponseDto getWalletBalance(Long userId) {
        String cacheKey = cachePrefix + userId;
        WalletResponseDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);

        if (cachedBalance != null) {
            logger.info("Cache hit for user ID: {}", userId);
            return cachedBalance;
        }

        Wallet wallet = getWalletByUserId(userId);
        return updateCacheAndNotify(wallet);
    }

    @Override
    public boolean walletExists(Long userId) {
        return walletRepository.findByUserId(userId).isPresent();
    }

    @Transactional
    @Override
    public void updateWalletBalance(WalletResponseDto WalletResponseDto) {
        Wallet wallet = getWalletByUserId(WalletResponseDto.getUserId());
        wallet.setUsdBalance(WalletResponseDto.getUsdBalance());
        wallet.setBtcBalance(WalletResponseDto.getBtcBalance());
        walletRepository.save(wallet);
        updateCacheAndNotify(wallet);
        logger.info("Wallet and cache updated, balance published for user ID: {}", WalletResponseDto.getUserId());
    }

    private WalletResponseDto updateCacheAndNotify(Wallet wallet) {
        WalletResponseDto walletResponseDto = WalletResponseDto.builder()
                .userId(wallet.getUserId())
                .usdBalance(wallet.getUsdBalance())
                .btcBalance(wallet.getBtcBalance())
                .build();

        String cacheKey = cachePrefix + wallet.getUserId();
        redisTemplate.opsForValue().set(cacheKey, walletResponseDto);
        logger.info("Cache updated for user ID: {}", wallet.getUserId());
        walletBalancePublisher.publishWalletBalance(walletResponseDto);
        logger.info("Balance published for user ID: {}", wallet.getUserId());
        return walletResponseDto;
    }
}
