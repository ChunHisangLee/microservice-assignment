package com.jack.outboxservice.config;

import com.jack.common.constants.WalletConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class RabbitMQConfig {
    @Bean
    public Queue walletCreateQueue() {
        log.info("Creating queue: {}", WalletConstants.WALLET_CREATE_QUEUE);
        return new Queue(WalletConstants.WALLET_CREATE_QUEUE, true);
    }

    // Define the exchange
    @Bean
    public TopicExchange walletExchange() {
        log.info("Creating exchange: {}", WalletConstants.WALLET_EXCHANGE);
        return new TopicExchange(WalletConstants.WALLET_EXCHANGE);
    }

    // Bind the creation queue to the exchange with its routing key
    @Bean
    public Binding bindingCreateQueue() {
        log.info("Binding queue {} to exchange {} with routing key {}",
                WalletConstants.WALLET_CREATE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_CREATE_ROUTING_KEY);

        return BindingBuilder.bind(walletCreateQueue())
                .to(walletExchange())
                .with(WalletConstants.WALLET_CREATE_ROUTING_KEY);
    }
}
