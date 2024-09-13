package com.jack.walletservice.listener;

import com.jack.walletservice.message.WalletCreationMessage;
import com.jack.walletservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WalletCreateListener {

    private static final Logger logger = LoggerFactory.getLogger(WalletCreateListener.class);
    private final WalletService walletService;

    public WalletCreateListener(WalletService walletService) {
        this.walletService = walletService;
    }

    // Listen to the queue for wallet creation messages
    @RabbitListener(queues = "${app.wallet.queue.create}")
    public void handleWalletCreation(WalletCreationMessage message) {
        logger.info("Received Wallet Creation message for user ID: {}", message.getUserId());

        try {
            if (walletService.walletExists(message.getUserId())) {
                logger.info("Wallet already exists for user ID: {}. Skipping creation.", message.getUserId());
                return;
            }

            walletService.createWallet(message.getUserId());
            walletService.creditWallet(message.getUserId(), message.getInitialBalance());
            logger.info("Wallet created and credited with initial balance for user ID: {}", message.getUserId());
        } catch (Exception e) {
            logger.error("Failed to process wallet creation message for user ID: {}. Error: {}", message.getUserId(), e.getMessage(), e);
        }
    }
}
