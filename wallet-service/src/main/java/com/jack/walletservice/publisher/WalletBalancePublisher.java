package com.jack.walletservice.publisher;

import com.jack.common.dto.response.WalletBalanceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WalletBalancePublisher {

    private static final Logger logger = LoggerFactory.getLogger(WalletBalancePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.wallet.exchange}")
    private String walletExchange;

    @Value("${app.wallet.routing-key.balance}")
    private String walletBalanceRoutingKey;

    public WalletBalancePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishWalletBalance(WalletBalanceDto WalletBalanceDto) {
        rabbitTemplate.convertAndSend(walletExchange, walletBalanceRoutingKey, WalletBalanceDto);
        logger.info("Published wallet balance for user ID: {} to RabbitMQ", WalletBalanceDto.getUserId());
    }
}
