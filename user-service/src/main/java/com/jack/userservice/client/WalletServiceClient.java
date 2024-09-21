package com.jack.userservice.client;

import com.jack.common.dto.response.WalletResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "wallet-service", url = "${WALLET_SERVICE_URL:https://wallet-service:8082}")
public interface WalletServiceClient {

    @PostMapping("/api/wallet/{userId}")
    WalletResponseDto getWalletBalance(@PathVariable("userId") Long userId);
}
