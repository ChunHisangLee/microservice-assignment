package com.jack.walletservice.listener;

import com.jack.common.dto.response.WalletUpdateMessageDto;
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
    public void handleWalletUpdate(WalletUpdateMessageDto message) {
        logger.info("Received Wallet Update for UserID: {}. USD: {}, BTC: {}", message.getUserId(), message.getUsdAmount(), message.getBtcAmount());

        try {
            // Validate the incoming message before proceeding
            if (message.getUsdAmount() < 0 || message.getBtcAmount() < 0) {
                logger.error("Invalid update amounts for user ID: {}. USD: {}, BTC: {}. Amounts must be non-negative.",
                        message.getUserId(), message.getUsdAmount(), message.getBtcAmount());
                return;
            }

            // Delegate the update logic to the WalletService
            walletService.updateWallet(message.getUserId(), message.getUsdAmount(), message.getBtcAmount());
            logger.info("Wallet updated successfully for user ID: {}", message.getUserId());
        } catch (Exception e) {
            logger.error("Failed to update wallet for user ID: {}. Error: {}", message.getUserId(), e.getMessage(), e);
        }
    }
}
