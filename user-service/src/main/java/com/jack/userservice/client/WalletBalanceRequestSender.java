package com.jack.userservice.client;

import com.jack.walletservice.message.WalletBalanceRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WalletBalanceRequestSender {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.wallet.exchange}")
    private String walletExchange;

    @Value("${app.wallet.routing-key.balance}")
    private String walletBalanceRoutingKey;

    @Value("${app.wallet.reply-to-queue}")
    private String replyToQueue;

    public WalletBalanceRequestSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBalanceRequest(Long userId) {
        WalletBalanceRequest request = WalletBalanceRequest.builder()
                .userId(userId)
                .build();

        // Send the request to the wallet service via RabbitMQ
        rabbitTemplate.convertAndSend(walletExchange, walletBalanceRoutingKey, request, message -> {
            // Set the reply-to queue where the response will be sent
            message.getMessageProperties().setReplyTo(replyToQueue);
            return message;
        });
    }
}
