package com.jack.outboxservice.config;

import com.jack.common.constants.TransactionConstants;
import com.jack.common.constants.UserConstants;
import com.jack.common.constants.WalletConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class RabbitMQConfig {
    // Wallet Service Configuration
    @Bean
    public Queue walletCreateQueue() {
        log.info("Creating walletCreateQueue: {}", WalletConstants.WALLET_CREATE_QUEUE);
        return QueueBuilder.durable(WalletConstants.WALLET_CREATE_QUEUE).build();
    }

    @Bean
    public Queue walletUpdateQueue() {
        log.info("Creating walletUpdateQueue: {}", WalletConstants.WALLET_UPDATE_QUEUE);
        return QueueBuilder.durable(WalletConstants.WALLET_UPDATE_QUEUE).build();
    }

    @Bean
    public TopicExchange walletExchange() {
        log.info("Creating walletExchange: {}", WalletConstants.WALLET_EXCHANGE);
        return ExchangeBuilder.topicExchange(WalletConstants.WALLET_EXCHANGE)
                .durable(true)
                .build();
    }

    // Bind the creation queue to the exchange with its routing key
    @Bean
    public Binding bindingWalletCreateQueue() {
        log.info("Binding WalletCreateQueue {} to exchange {} with routing key {}",
                WalletConstants.WALLET_CREATE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_CREATE_ROUTING_KEY);

        return BindingBuilder.bind(walletCreateQueue())
                .to(walletExchange())
                .with(WalletConstants.WALLET_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingWalletUpdateQueue() {
        log.info("Binding WalletUpdateQueue {} to exchange {} with routing key {}",
                WalletConstants.WALLET_UPDATE_QUEUE,
                WalletConstants.WALLET_EXCHANGE,
                WalletConstants.WALLET_UPDATE_ROUTING_KEY);

        return BindingBuilder.bind(walletUpdateQueue())
                .to(walletExchange())
                .with(WalletConstants.WALLET_UPDATE_ROUTING_KEY);
    }
    // User Service Configuration
    @Bean
    public Queue userCreateQueue() {
        log.info("Creating userCreateQueue: {}", UserConstants.USER_CREATE_QUEUE);
        return QueueBuilder.durable(UserConstants.USER_CREATE_QUEUE).build();
    }

    @Bean
    public Queue userUpdateQueue() {
        log.info("Creating userUpdateQueue: {}", UserConstants.USER_UPDATE_QUEUE);
        return QueueBuilder.durable(UserConstants.USER_UPDATE_QUEUE).build();
    }


    @Bean
    public TopicExchange userExchange() {
        log.info("Creating userExchange: {}", UserConstants.USER_EXCHANGE);
        return ExchangeBuilder.topicExchange(UserConstants.USER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Binding bindingUserCreateQueue() {
        log.info("Binding UserCreateQueue {} to exchange {} with routing key {}",
                UserConstants.USER_CREATE_QUEUE,
                UserConstants.USER_EXCHANGE,
                UserConstants.USER_CREATE_ROUTING_KEY);

        return BindingBuilder.bind(userCreateQueue())
                .to(userExchange())
                .with(UserConstants.USER_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingUserUpdateQueue() {
        log.info("Binding UserUpdateQueue {} to exchange {} with routing key {}",
                UserConstants.USER_UPDATE_QUEUE,
                UserConstants.USER_EXCHANGE,
                UserConstants.USER_UPDATE_ROUTING_KEY);

        return BindingBuilder.bind(userUpdateQueue())
                .to(userExchange())
                .with(UserConstants.USER_UPDATE_ROUTING_KEY);
    }


    // Transaction Service Configuration
    @Bean
    public Queue transactionCreateQueue() {
        log.info("Creating transactionCreateQueue: {}", TransactionConstants.TRANSACTION_CREATE_QUEUE);
        return QueueBuilder.durable(TransactionConstants.TRANSACTION_CREATE_QUEUE)
                .build();
    }

    @Bean
    public Queue transactionUpdateQueue() {
        log.info("Creating transactionUpdateQueue: {}", TransactionConstants.TRANSACTION_UPDATE_QUEUE);
        return QueueBuilder.durable(TransactionConstants.TRANSACTION_UPDATE_QUEUE).build();
    }

    @Bean
    public TopicExchange transactionExchange() {
        log.info("Creating transactionExchange: {}", TransactionConstants.TRANSACTION_EXCHANGE);
        return ExchangeBuilder.topicExchange(TransactionConstants.TRANSACTION_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Binding bindingTransactionCreateQueue() {
        log.info("Binding TransactionCreateQueue {} to exchange {} with routing key {}",
                TransactionConstants.TRANSACTION_CREATE_QUEUE,
                TransactionConstants.TRANSACTION_EXCHANGE,
                TransactionConstants.TRANSACTION_CREATE_ROUTING_KEY);

        return BindingBuilder.bind(transactionCreateQueue())
                .to(transactionExchange())
                .with(TransactionConstants.TRANSACTION_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingTransactionUpdateQueue() {
        log.info("Binding TransactionUpdateQueue {} to exchange {} with routing key {}",
                TransactionConstants.TRANSACTION_UPDATE_QUEUE,
                TransactionConstants.TRANSACTION_EXCHANGE,
                TransactionConstants.TRANSACTION_UPDATE_ROUTING_KEY);

        return BindingBuilder.bind(transactionUpdateQueue())
                .to(transactionExchange())
                .with(TransactionConstants.TRANSACTION_UPDATE_ROUTING_KEY);
    }
}
