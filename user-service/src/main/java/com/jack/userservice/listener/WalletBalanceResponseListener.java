package com.jack.userservice.listener;

import com.jack.common.constants.TransactionConstants;
import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class WalletBalanceResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(WalletBalanceResponseListener.class);

    private final RedisTemplate<String, WalletResponseDto> redisTemplate;

    public WalletBalanceResponseListener(RedisTemplate<String, WalletResponseDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = WalletConstants.WALLET_REPLY_TO_QUEUE)
    public void handleWalletBalanceResponse(WalletResponseDto walletResponse) {
        String cacheKey = WalletConstants.WALLET_CACHE_PREFIX + walletResponse.getUserId();

        // Cache the wallet balance received from wallet-service
        redisTemplate.opsForValue().set(cacheKey, walletResponse, TransactionConstants.TRANSACTION_CACHE_TTL, TimeUnit.MINUTES);
        logger.info("Wallet balance for user ID {} cached in Redis.", walletResponse.getUserId());
    }
}
