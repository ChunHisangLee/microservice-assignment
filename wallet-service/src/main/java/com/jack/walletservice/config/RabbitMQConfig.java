package com.jack.walletservice.config;

import com.jack.common.constants.WalletConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class RabbitMQConfig {
    // Define Exchange
    @Bean
    public TopicExchange walletExchange() {
        log.info("Creating exchange: {}", WalletConstants.WALLET_EXCHANGE);
        return ExchangeBuilder.topicExchange(WalletConstants.WALLET_EXCHANGE)
                .durable(true)
                .build();
    }

    // Define Queues
    @Bean
    public Queue walletCreateQueue() {
        log.info("Creating walletCreateQueue: {}", WalletConstants.WALLET_CREATE_QUEUE);
        return QueueBuilder.durable(WalletConstants.WALLET_CREATE_QUEUE)
                .build();
    }

    @Bean
    public Queue walletUpdateQueue() {
        log.info("Creating walletUpdateQueue: {}", WalletConstants.WALLET_UPDATE_QUEUE);
        return QueueBuilder.durable(WalletConstants.WALLET_UPDATE_QUEUE)
                .build();
    }

    @Bean
    public Queue walletBalanceQueue() {
        log.info("Creating walletBalanceQueue: {}", WalletConstants.WALLET_BALANCE_QUEUE);
        return QueueBuilder.durable(WalletConstants.WALLET_BALANCE_QUEUE)
                .build();
    }

    // Bindings
    @Bean
    public Binding bindingCreateQueue(Queue walletCreateQueue, TopicExchange walletExchange) {
        log.info("Binding CreateQueue '{}' to exchange '{}' with routing key '{}'",
                WalletConstants.WALLET_CREATE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_CREATE_ROUTING_KEY);
        return BindingBuilder.bind(walletCreateQueue)
                .to(walletExchange)
                .with(WalletConstants.WALLET_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingUpdateQueue(Queue walletUpdateQueue, TopicExchange walletExchange) {
        log.info("Binding UpdateQueue '{}' to exchange '{}' with routing key '{}'",
                WalletConstants.WALLET_UPDATE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_UPDATE_ROUTING_KEY);
        return BindingBuilder.bind(walletUpdateQueue)
                .to(walletExchange)
                .with(WalletConstants.WALLET_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingBalanceQueue(Queue walletBalanceQueue, TopicExchange walletExchange) {
        log.info("Binding BalanceQueue '{}' to exchange '{}' with routing key '{}'",
                WalletConstants.WALLET_BALANCE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_BALANCE_ROUTING_KEY);
        return BindingBuilder.bind(walletBalanceQueue)
                .to(walletExchange)
                .with(WalletConstants.WALLET_BALANCE_ROUTING_KEY);
    }
}
