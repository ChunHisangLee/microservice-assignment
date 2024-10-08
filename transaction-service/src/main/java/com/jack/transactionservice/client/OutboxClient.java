package com.jack.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "outbox-service", url = "${OUTBOX_SERVICE_URL:https://outbox-service:8083}")
public interface OutboxClient {

    @PostMapping("/api/outbox/sendTransactionEvent")
    void sendTransactionEvent(@RequestParam("transactionId") Long transactionId,
                              @RequestParam("userId") Long userId,
                              @RequestParam("btcAmount") BigDecimal btcAmount,
                              @RequestParam("usdAmount") BigDecimal usdAmount);
}
