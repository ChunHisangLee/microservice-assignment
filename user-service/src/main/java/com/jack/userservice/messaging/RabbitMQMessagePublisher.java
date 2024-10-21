package com.jack.userservice.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String eventType, String payload) {
        // Assuming eventType corresponds to the RabbitMQ exchange or routing key
        // Adjust as per your RabbitMQ setup (exchange, routing key, etc.)
        String exchange = "user.events.exchange"; // Define your exchange
        String routingKey = eventType; // Or map eventType to specific routing keys
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
