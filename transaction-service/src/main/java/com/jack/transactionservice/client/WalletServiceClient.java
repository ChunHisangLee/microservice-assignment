package com.jack.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wallet-service", url = "${WALLET_SERVICE_URL:https://wallet-service:8082}")
public interface WalletServiceClient {

    @PostMapping("/wallet/updateBalance")
    void updateWalletBalance(@RequestParam("userId") Long userId,
                             @RequestParam("usdBalance") double usdBalance,
                             @RequestParam("btcBalance") double btcBalance);
}
