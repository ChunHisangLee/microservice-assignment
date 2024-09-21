package com.jack.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "wallet-service", url = "${WALLET_SERVICE_URL:https://wallet-service:8082}")
public interface WalletServiceClient {

    @PostMapping("/api/wallet/{userId}")
    void updateWalletBalance(@PathVariable("userId") Long userId,
                             @RequestParam("usdBalance") BigDecimal usdBalance,
                             @RequestParam("btcBalance") BigDecimal btcBalance);
}
