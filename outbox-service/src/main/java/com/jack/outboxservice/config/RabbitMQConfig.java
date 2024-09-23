package com.jack.outboxservice.config;

import com.jack.common.constants.WalletConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Bean
    public Queue walletCreateQueue() {
        logger.info("Creating queue: {}", WalletConstants.WALLET_CREATE_QUEUE);
        return new Queue(WalletConstants.WALLET_CREATE_QUEUE, true);
    }

    // Define the exchange
    @Bean
    public TopicExchange walletExchange() {
        logger.info("Creating exchange: {}", WalletConstants.WALLET_EXCHANGE);
        return new TopicExchange(WalletConstants.WALLET_EXCHANGE);
    }

    // Bind the creation queue to the exchange with its routing key
    @Bean
    public Binding bindingCreateQueue() {
        logger.info("Binding queue {} to exchange {} with routing key {}",
                WalletConstants.WALLET_CREATE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_CREATE_ROUTING_KEY);

        return BindingBuilder.bind(walletCreateQueue())
                .to(walletExchange())
                .with(WalletConstants.WALLET_CREATE_ROUTING_KEY);
    }
}
