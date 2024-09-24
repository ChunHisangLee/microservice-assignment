package com.jack.transactionservice.client;

import com.jack.common.dto.response.WalletResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/api/wallets/{userId}/balances")
    WalletResponseDto getWalletBalance(@PathVariable("userId") Long userId);
}
