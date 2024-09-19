package com.jack.outboxservice.config;

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

    @Value("${app.wallet.routing-key.create}")
    private String walletCreateRoutingKey;

    @Value("${app.wallet.exchange}")
    private String walletExchange;

    // Define the create queue
    @Bean
    public Queue walletCreateQueue() {
        logger.info("Creating queue: {}", walletCreateQueue);
        return new Queue(walletCreateQueue, true);
    }

    // Define the exchange
    @Bean
    public TopicExchange walletExchange() {
        logger.info("Creating exchange: {}", walletExchange);
        return new TopicExchange(walletExchange);
    }

    // Bind the creation queue to the exchange with its routing key
    @Bean
    public Binding bindingCreateQueue() {
        logger.info("Binding queue {} to exchange {} with routing key {}", walletCreateQueue, walletExchange, walletCreateRoutingKey);
        return BindingBuilder.bind(walletCreateQueue()).to(walletExchange()).with(walletCreateRoutingKey);
    }
}
