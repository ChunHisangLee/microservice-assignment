package com.jack.userservice.messaging;

import com.jack.common.constants.UserConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class RabbitMQMessagePublisher implements MessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String routingKey, String payload) {
        String exchange = UserConstants.USER_EXCHANGE;

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("Published message to exchange '{}', routing key '{}', payload '{}'", exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish message to exchange '{}', routing key '{}', payload '{}', error: {}",
                    exchange, routingKey, payload, e.getMessage());
        }
    }
}
