package com.jack.userservice.config;

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

    @Value("${app.wallet.queue.create}")
    private String walletCreateQueue;

    @Value("${app.wallet.queue.update}")
    private String walletUpdateQueue;

    @Value("${app.wallet.queue.balance}")
    private String walletBalanceQueue;

    @Value("${app.wallet.exchange}")
    private String walletExchange;

    @Value("${app.wallet.routing-key.create}")
    private String walletCreateRoutingKey;

    @Value("${app.wallet.routing-key.update}")
    private String walletUpdateRoutingKey;

    @Value("${app.wallet.routing-key.balance}")
    private String walletBalanceRoutingKey;

    // Define queues
    @Bean
    public Queue walletCreateQueue() {
        logger.info("Creating queue: {}", walletCreateQueue);
        return new Queue(walletCreateQueue, true); // Durable queue for persistence
    }


    @Bean
    public Queue walletBalanceQueue() {
        logger.info("The balance of queue: {}", walletBalanceQueue);
        return new Queue(walletBalanceQueue, true);
    }

    // Define exchange
    @Bean
    public TopicExchange walletExchange() {
        logger.info("Creating exchange: {}", walletExchange);
        return new TopicExchange(walletExchange);
    }

    // Bind queues to exchange with respective routing keys
    @Bean
    public Binding bindingCreateQueue() {
        logger.info("Binding the creation of queue {} to exchange {} with routing key {}", walletCreateQueue, walletExchange, walletCreateRoutingKey);
        return BindingBuilder.bind(walletCreateQueue()).to(walletExchange()).with(walletCreateRoutingKey);
    }

    @Bean
    public Binding bindingBalanceQueue() {
        logger.info("Binding the balance of queue {} to exchange {} with routing key {}", walletCreateQueue, walletExchange, walletBalanceRoutingKey);
        return BindingBuilder.bind(walletBalanceQueue()).to(walletExchange()).with(walletBalanceRoutingKey);
    }
}
