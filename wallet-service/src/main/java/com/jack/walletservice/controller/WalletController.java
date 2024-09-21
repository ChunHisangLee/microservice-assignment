package com.jack.walletservice.controller;

import com.jack.walletservice.dto.WalletDto;
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

    @PutMapping("/{userId}")
    public ResponseEntity<WalletDto> updateWallet(@PathVariable Long userId,
                                                  @RequestParam("usdBalance") double usdBalance,
                                                  @RequestParam("btcBalance") double btcBalance) {
        WalletDto updatedWallet = walletService.updateWallet(userId, BigDecimal.valueOf(usdBalance), BigDecimal.valueOf(btcBalance));
        return ResponseEntity.ok(updatedWallet);
    }
}
