package com.jack.walletservice.config;

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
        return new Queue(walletCreateQueue, true); // Durable queue for persistence
    }

    @Bean
    public Queue walletUpdateQueue() {
        return new Queue(walletUpdateQueue, true);
    }

    @Bean
    public Queue walletBalanceQueue() {
        return new Queue(walletBalanceQueue, true);
    }

    // Define exchange
    @Bean
    public TopicExchange walletExchange() {
        return new TopicExchange(walletExchange);
    }

    // Bind queues to exchange with respective routing keys
    @Bean
    public Binding bindingCreateQueue() {
        return BindingBuilder.bind(walletCreateQueue()).to(walletExchange()).with(walletCreateRoutingKey);
    }

    @Bean
    public Binding bindingUpdateQueue() {
        return BindingBuilder.bind(walletUpdateQueue()).to(walletExchange()).with(walletUpdateRoutingKey);
    }

    @Bean
    public Binding bindingBalanceQueue() {
        return BindingBuilder.bind(walletBalanceQueue()).to(walletExchange()).with(walletBalanceRoutingKey);
    }
}
