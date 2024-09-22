package com.jack.userservice.client;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletBalanceMessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletBalanceRequestSender {
    private final RabbitTemplate rabbitTemplate;

    public WalletBalanceRequestSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBalanceRequest(Long userId) {
        // Build the new WalletBalanceMessageDto instead of WalletBalanceRequest
        WalletBalanceMessageDto request = WalletBalanceMessageDto.builder()
                .userId(userId)
                .build();

        // Send the request to the wallet service via RabbitMQ
        rabbitTemplate.convertAndSend(WalletConstants.WALLET_EXCHANGE, WalletConstants.WALLET_BALANCE_ROUTING_KEY, request, message -> {
            // Set the reply-to queue where the response will be sent
            message.getMessageProperties().setReplyTo(WalletConstants.WALLET_REPLY_TO_QUEUE);
            return message;
        });
    }
}
