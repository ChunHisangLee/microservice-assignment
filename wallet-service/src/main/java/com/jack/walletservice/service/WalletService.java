package com.jack.walletservice.service;

import com.jack.walletservice.dto.WalletBalanceDTO;
import com.jack.walletservice.entity.Wallet;

public interface WalletService {
    Wallet createWallet(Long userId);  // Use userId to create a wallet

    void creditWallet(Long userId, Double amount);

    Wallet getWalletByUserId(Long userId);

    void updateWallet(Long userId, Double usdAmount, Double btcAmount);

    void updateWalletBalance(WalletBalanceDTO walletBalanceDTO);

    void debitWallet(Long userId, Double amount);

    WalletBalanceDTO getWalletBalance(Long userId);

    boolean walletExists(Long userId);
}
