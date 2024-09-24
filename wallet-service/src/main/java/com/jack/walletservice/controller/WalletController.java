package com.jack.walletservice.controller;

import com.jack.common.dto.response.WalletResponseDto;
import com.jack.walletservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // Update wallet balances
    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateWalletBalance(@PathVariable Long userId,
                                             @RequestParam("usdBalance") BigDecimal usdBalance,
                                             @RequestParam("btcBalance") BigDecimal btcBalance) {
        walletService.updateWallet(userId, usdBalance, btcBalance);
        return ResponseEntity.ok().build();
    }

    // Get wallet balance by userId
    @GetMapping("/{userId}/balances")
    public ResponseEntity<WalletResponseDto> getWalletBalance(@PathVariable Long userId) {
        WalletResponseDto walletResponseDto = walletService.getWalletBalance(userId);
        return ResponseEntity.ok(walletResponseDto);
    }
}
