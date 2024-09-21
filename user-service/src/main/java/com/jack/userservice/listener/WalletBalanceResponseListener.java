package com.jack.userservice.listener;

import com.jack.common.dto.response.WalletResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class WalletBalanceResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(WalletBalanceResponseListener.class);

    private final RedisTemplate<String, WalletResponseDto> redisTemplate;

    @Value("${app.wallet.cache-prefix}")
    private String cachePrefix;

    public WalletBalanceResponseListener(RedisTemplate<String, WalletResponseDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = "${app.wallet.reply-to-queue}")
    public void receiveWalletBalance(WalletResponseDto walletBalance) {
        if (walletBalance != null && walletBalance.getUserId() != null) {
            String cacheKey = cachePrefix + walletBalance.getUserId();

            // Check if the balance is already cached
            WalletResponseDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);

            if (cachedBalance != null) {
                logger.info("Fetched cached balance for user ID: {} - USD: {}, BTC: {}",
                        cachedBalance.getUserId(), cachedBalance.getUsdBalance(), cachedBalance.getBtcBalance());
            }

            // Update Redis with the new balance
            redisTemplate.opsForValue().set(cacheKey, walletBalance);
            logger.info("Updated Redis cache with new balance for user ID: {} - USD: {}, BTC: {}",
                    walletBalance.getUserId(), walletBalance.getUsdBalance(), walletBalance.getBtcBalance());

        } else {
            logger.error("Received invalid or null wallet balance response: {}", walletBalance);
        }
    }
}
