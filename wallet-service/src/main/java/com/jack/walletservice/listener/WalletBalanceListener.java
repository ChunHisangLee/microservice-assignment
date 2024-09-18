package com.jack.walletservice.listener;

import com.jack.common.dto.response.WalletBalanceMessageDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.walletservice.entity.Wallet;
import com.jack.walletservice.exception.WalletNotFoundException;
import com.jack.walletservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class WalletBalanceListener {

    private static final Logger logger = LoggerFactory.getLogger(WalletBalanceListener.class);
    private final WalletService walletService;
    private final RabbitTemplate rabbitTemplate;

    public WalletBalanceListener(WalletService walletService, RabbitTemplate rabbitTemplate) {
        this.walletService = walletService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "${app.wallet.queue.balance}")
    public void handleWalletBalanceRequest(WalletBalanceMessageDto WalletBalanceMessageDto, Message message) {
        Long userId = WalletBalanceMessageDto.getUserId();
        logger.info("Received Wallet Balance Request for UserID: {}", userId);

        // Get the replyTo queue from the message properties
        MessageProperties messageProperties = message.getMessageProperties();
        String replyToQueue = messageProperties.getReplyTo();

        if (replyToQueue == null) {
            logger.error("No reply-to queue specified in the message for user ID: {}", userId);
            return;
        }

        // Validate userId
        if (userId == null) {
            logger.error("Invalid Wallet Balance Request: userId is null");
            String errorMessage = "Invalid request: userId is null";
            rabbitTemplate.convertAndSend(replyToQueue, errorMessage);
            return;
        }

        try {
            Wallet wallet = walletService.getWalletByUserId(userId);

            WalletResponseDto responseDTO = WalletResponseDto.builder()
                    .userId(wallet.getUserId())
                    .usdBalance(wallet.getUsdBalance())
                    .btcBalance(wallet.getBtcBalance())
                    .build();

            // Send the successful response to the replyToQueue
            rabbitTemplate.convertAndSend(replyToQueue, responseDTO);
            logger.info("Wallet balance sent to replyToQueue: {} for UserID: {}", replyToQueue, userId);

        } catch (WalletNotFoundException e) {
            logger.error("Wallet not found for user ID: {}", userId);
            // Send an error message to the replyToQueue
            String errorMessage = "Wallet not found for user ID: " + userId;
            rabbitTemplate.convertAndSend(replyToQueue, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to retrieve wallet balance for user ID: {}. Error: {}", userId, e.getMessage(), e);
            // Send a generic error message to the replyToQueue
            String errorMessage = "Failed to retrieve wallet balance for user ID: " + userId;
            rabbitTemplate.convertAndSend(replyToQueue, errorMessage);
        }
    }
}
