package com.jack.userservice.client;

import com.jack.common.dto.request.OutboxRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "outbox-service", url = "${OUTBOX_SERVICE_URL:https://auth-service:8083}")
public interface OutboxServiceClient {

    @PostMapping("/api/outbox")
    void sendOutboxEvent(@RequestBody OutboxRequestDto outboxRequestDto);
}
