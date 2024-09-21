package com.jack.walletservice.publisher;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletBalancePublisher {
    private static final Logger logger = LoggerFactory.getLogger(WalletBalancePublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public WalletBalancePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishWalletBalance(WalletResponseDto walletResponseDto) {
        // Publish to the normal balance queue
        rabbitTemplate.convertAndSend(WalletConstants.WALLET_EXCHANGE, WalletConstants.WALLET_BALANCE_ROUTING_KEY, walletResponseDto);
        logger.info("Published wallet balance for user ID: {} to RabbitMQ balance queue", walletResponseDto.getUserId());

        // Additionally, publish to the reply-to queue
        rabbitTemplate.convertAndSend(WalletConstants.WALLET_REPLY_TO_QUEUE, walletResponseDto);
        logger.info("Published wallet balance for user ID: {} to RabbitMQ reply-to queue", walletResponseDto.getUserId());
    }
}
