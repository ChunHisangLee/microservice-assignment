package com.jack.walletservice.listener;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletUpdateMessageDto;
import com.jack.walletservice.service.WalletService;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Log4j2
public class WalletUpdateListener {
    private final WalletService walletService;

    public WalletUpdateListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @RabbitListener(queues = WalletConstants.WALLET_UPDATE_QUEUE)
    public void handleWalletUpdate(WalletUpdateMessageDto message) {
        log.info("Received Wallet Update for UserID: {}. USD: {}, BTC: {}", message.getUserId(), message.getUsdAmount(), message.getBtcAmount());

        try {
            // Validate the incoming message before proceeding
            if (message.getUsdAmount().compareTo(BigDecimal.ZERO) < 0 ||
                    message.getBtcAmount().compareTo(BigDecimal.ZERO) < 0) {
                log.error("Invalid update amounts for user ID: {}. USD: {}, BTC: {}. Amounts must be non-negative.",
                        message.getUserId(), message.getUsdAmount(), message.getBtcAmount());
                return;
            }

            walletService.updateWallet(message.getUserId(), message.getUsdAmount(), message.getBtcAmount());
            log.info("Wallet updated successfully for user ID: {}", message.getUserId());
        } catch (Exception e) {
            log.error("Failed to update wallet for user ID: {}. Error: {}", message.getUserId(), e.getMessage(), e);
        }
    }
}
