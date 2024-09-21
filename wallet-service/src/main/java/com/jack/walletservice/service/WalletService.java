package com.jack.walletservice.service;

import com.jack.common.dto.response.WalletResponseDto;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.walletservice.entity.Wallet;

import java.math.BigDecimal;

public interface WalletService {

    void createWallet(WalletCreateMessageDto message);

    Wallet getWalletByUserId(Long userId);

    void updateWallet(Long userId, BigDecimal usdAmount, BigDecimal btcAmount);

    void updateWalletBalance(WalletResponseDto WalletResponseDto);

    void debitWallet(Long userId, BigDecimal amount);

    WalletResponseDto getWalletBalance(Long userId);

    boolean walletExists(Long userId);
}
