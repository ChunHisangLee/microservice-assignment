package com.jack.transactionservice.config;

import com.jack.common.constants.TransactionConstants;
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

    // Define the create queue
    @Bean
    public Queue transactionCreateQueue() {
        log.info("Creating CreateQueue: {}", TransactionConstants.TRANSACTION_CREATE_QUEUE);
        return new Queue(TransactionConstants.TRANSACTION_CREATE_QUEUE, true); // Durable queue for persistence
    }

    // Define the update queue
    @Bean
    public Queue transactionUpdateQueue() {
        log.info("Creating UpdateQueue: {}", TransactionConstants.TRANSACTION_UPDATE_QUEUE);
        return new Queue(TransactionConstants.TRANSACTION_UPDATE_QUEUE, true);
    }

    // Define the balance queue
    @Bean
    public Queue transactionBalanceQueue() {
        log.info("Creating BalanceQueue: {}", TransactionConstants.TRANSACTION_BALANCE_QUEUE);
        return new Queue(TransactionConstants.TRANSACTION_BALANCE_QUEUE, true);
    }

    // Define the exchange
    @Bean
    public TopicExchange transactionExchange() {
        log.info("Creating exchange: {}", TransactionConstants.TRANSACTION_EXCHANGE);
        return new TopicExchange(TransactionConstants.TRANSACTION_EXCHANGE);
    }

    // Bind the create queue to the exchange with the routing key
    @Bean
    public Binding bindingCreateQueue() {
        log.info("Binding CreateQueue {} to exchange {} with routing key {}",
                TransactionConstants.TRANSACTION_CREATE_QUEUE,
                TransactionConstants.TRANSACTION_EXCHANGE,
                TransactionConstants.TRANSACTION_CREATE_ROUTING_KEY);
        return BindingBuilder.bind(transactionCreateQueue())
                .to(transactionExchange())
                .with(TransactionConstants.TRANSACTION_CREATE_ROUTING_KEY);
    }

    // Bind the update queue to the exchange with the routing key
    @Bean
    public Binding bindingUpdateQueue() {
        log.info("Binding UpdateQueue {} to exchange {} with routing key {}",
                TransactionConstants.TRANSACTION_UPDATE_QUEUE,
                TransactionConstants.TRANSACTION_EXCHANGE,
                TransactionConstants.TRANSACTION_UPDATE_ROUTING_KEY);
        return BindingBuilder.bind(transactionUpdateQueue())
                .to(transactionExchange())
                .with(TransactionConstants.TRANSACTION_UPDATE_ROUTING_KEY);
    }

    // Bind the balance queue to the exchange with the routing key
    @Bean
    public Binding bindingBalanceQueue() {
        log.info("Binding BalanceQueue {} to exchange {} with routing key {}",
                TransactionConstants.TRANSACTION_BALANCE_QUEUE,
                TransactionConstants.TRANSACTION_EXCHANGE,
                TransactionConstants.TRANSACTION_BALANCE_ROUTING_KEY);
        return BindingBuilder.bind(transactionBalanceQueue())
                .to(transactionExchange())
                .with(TransactionConstants.TRANSACTION_BALANCE_ROUTING_KEY);
    }
}
