package com.jack.userservice.config;

import com.jack.common.constants.UserConstants;
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
    @Bean
    public TopicExchange userExchange() {
        log.info("Creating userExchange: {}", UserConstants.USER_EXCHANGE);
        return ExchangeBuilder.topicExchange(UserConstants.USER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue userCreateQueue() {
        log.info("Creating userCreateQueue: {}", UserConstants.USER_CREATE_QUEUE);
        return QueueBuilder.durable(UserConstants.USER_CREATE_QUEUE)
                .build();
    }

    @Bean
    public Queue userUpdateQueue() {
        log.info("Creating userUpdateQueue: {}", UserConstants.USER_UPDATE_QUEUE);
        return QueueBuilder.durable(UserConstants.USER_UPDATE_QUEUE)
                .build();
    }

    @Bean
    public Binding bindingCreateQueue(Queue userCreateQueue, TopicExchange userExchange) {
        log.info("Binding CreateQueue '{}' to exchange '{}' with routing key '{}'",
                UserConstants.USER_CREATE_QUEUE,
                UserConstants.USER_EXCHANGE,
                UserConstants.USER_CREATE_ROUTING_KEY);
        return BindingBuilder.bind(userCreateQueue)
                .to(userExchange)
                .with(UserConstants.USER_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingUpdateQueue(Queue userUpdateQueue, TopicExchange userExchange) {
        log.info("Binding UpdateQueue '{}' to exchange '{}' with routing key '{}'",
                UserConstants.USER_UPDATE_QUEUE,
                UserConstants.USER_EXCHANGE,
                UserConstants.USER_UPDATE_ROUTING_KEY);
        return BindingBuilder.bind(userUpdateQueue)
                .to(userExchange)
                .with(UserConstants.USER_UPDATE_ROUTING_KEY);
    }
}
