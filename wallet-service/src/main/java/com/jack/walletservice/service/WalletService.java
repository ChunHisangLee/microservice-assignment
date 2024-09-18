package com.jack.walletservice.service;

import com.jack.common.dto.response.WalletBalanceDto;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.walletservice.entity.Wallet;

public interface WalletService {

    void createWallet(WalletCreateMessageDto message);

    Wallet getWalletByUserId(Long userId);

    void updateWallet(Long userId, Double usdAmount, Double btcAmount);

    void updateWalletBalance(WalletBalanceDto WalletBalanceDto);

    void debitWallet(Long userId, Double amount);

    WalletBalanceDto getWalletBalance(Long userId);

    boolean walletExists(Long userId);
}
