package com.jack.walletservice.listener;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletCreateMessageDto;
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
    @RabbitListener(queues = WalletConstants.WALLET_CREATE_QUEUE)
    public void handleWalletCreation(WalletCreateMessageDto message) {
        logger.info("Received Wallet Creation message for user ID: {}", message.getUserId());

        try {
            // Delegate the wallet creation to WalletService's createWallet method
            walletService.createWallet(message);
            logger.info("Wallet created successfully for user ID: {}", message.getUserId());
        } catch (Exception e) {
            logger.error("Failed to process wallet creation message for user ID: {}. Error: {}", message.getUserId(), e.getMessage(), e);
        }
    }
}
