package com.jack.walletservice.listener;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.walletservice.service.WalletService;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class WalletCreateListener {
    private final WalletService walletService;

    @Autowired
    public WalletCreateListener(WalletService walletService) {
        this.walletService = walletService;
    }

    // Listen to the queue for wallet creation messages
    @RabbitListener(queues = WalletConstants.WALLET_CREATE_QUEUE)
    public void handleWalletCreation(WalletCreateMessageDto walletCreateMessageDto) {
        log.info("Received Wallet Creation message for user ID: {}", walletCreateMessageDto.getUserId());

        try {
            // Validate message
            if (walletCreateMessageDto.getUserId() == null) {
                log.warn("User ID is null for wallet creation message");
                return;
            }

            // Delegate the wallet creation to WalletService's createWallet method
            walletService.createWallet(walletCreateMessageDto);
            log.info("Wallet created successfully for user ID: {}", walletCreateMessageDto.getUserId());
        } catch (Exception e) {
            log.error("Failed to process wallet creation message for user ID: {}. Error: {}", walletCreateMessageDto.getUserId(), e.getMessage(), e);
        }
    }
}
