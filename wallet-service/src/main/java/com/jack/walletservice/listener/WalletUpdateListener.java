package com.jack.walletservice.listener;

import com.jack.walletservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WalletUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(WalletUpdateListener.class);
    private final WalletService walletService;

    public WalletUpdateListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @RabbitListener(queues = "${app.wallet.queue.update}")
    public void handleWalletUpdate(Long userId, Double usdAmount, Double btcAmount) {
        logger.info("Received Wallet Update for UserID: {}. USD: {}, BTC: {}", userId, usdAmount, btcAmount);

        try {
            if (usdAmount < 0 || btcAmount < 0) {
                logger.error("Invalid update amounts for user ID: {}. USD: {}, BTC: {}. Amounts must be non-negative.", userId, usdAmount, btcAmount);
                return;
            }

            walletService.updateWallet(userId, usdAmount, btcAmount);
            logger.info("Wallet updated successfully for user ID: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to update wallet for user ID: {}. Error: {}", userId, e.getMessage(), e);
        }
    }
}
