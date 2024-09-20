package com.jack.transactionservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${app.transaction.queue.create}")
    private String transactionCreateQueue;

    @Value("${app.transaction.queue.update}")
    private String transactionUpdateQueue;

    @Value("${app.transaction.queue.balance}")
    private String transactionBalanceQueue;

    @Value("${app.transaction.exchange}")
    private String transactionExchange;

    @Value("${app.transaction.routing-key.create}")
    private String transactionCreateRoutingKey;

    @Value("${app.transaction.routing-key.update}")
    private String transactionUpdateRoutingKey;

    @Value("${app.transaction.routing-key.balance}")
    private String transactionBalanceRoutingKey;

    // Define the create queue
    @Bean
    public Queue transactionCreateQueue() {
        logger.info("Creating CreateQueue: {}", transactionCreateQueue);
        return new Queue(transactionCreateQueue, true); // Durable queue for persistence
    }

    // Define the update queue
    @Bean
    public Queue transactionUpdateQueue() {
        logger.info("Creating UpdateQueue: {}", transactionUpdateQueue);
        return new Queue(transactionUpdateQueue, true);
    }

    // Define the balance queue
    @Bean
    public Queue transactionBalanceQueue() {
        logger.info("Creating BalanceQueue: {}", transactionBalanceQueue);
        return new Queue(transactionBalanceQueue, true);
    }

    // Define the exchange
    @Bean
    public TopicExchange transactionExchange() {
        logger.info("Creating exchange: {}", transactionExchange);
        return new TopicExchange(transactionExchange);
    }

    // Bind the create queue to the exchange with the routing key
    @Bean
    public Binding bindingCreateQueue() {
        logger.info("Binding CreateQueue {} to exchange {} with routing key {}", transactionCreateQueue, transactionExchange, transactionCreateRoutingKey);
        return BindingBuilder.bind(transactionCreateQueue()).to(transactionExchange()).with(transactionCreateRoutingKey);
    }

    // Bind the update queue to the exchange with the routing key
    @Bean
    public Binding bindingUpdateQueue() {
        logger.info("Binding UpdateQueue {} to exchange {} with routing key {}", transactionUpdateQueue, transactionExchange, transactionUpdateRoutingKey);
        return BindingBuilder.bind(transactionUpdateQueue()).to(transactionExchange()).with(transactionUpdateRoutingKey);
    }

    // Bind the balance queue to the exchange with the routing key
    @Bean
    public Binding bindingBalanceQueue() {
        logger.info("Binding BalanceQueue {} to exchange {} with routing key {}", transactionBalanceQueue, transactionExchange, transactionBalanceRoutingKey);
        return BindingBuilder.bind(transactionBalanceQueue()).to(transactionExchange()).with(transactionBalanceRoutingKey);
    }
}
