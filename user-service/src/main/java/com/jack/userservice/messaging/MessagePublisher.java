package com.jack.userservice.messaging;

public interface MessagePublisher {
    void publish(String eventType, String payload);
}
