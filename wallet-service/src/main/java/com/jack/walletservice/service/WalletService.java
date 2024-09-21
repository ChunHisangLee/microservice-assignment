package com.jack.walletservice.service;

import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.common.dto.response.WalletResponseDto;

import java.math.BigDecimal;

public interface WalletService {

    void createWallet(WalletCreateMessageDto message);

    void updateWallet(Long userId, BigDecimal usdAmount, BigDecimal btcAmount);

    WalletResponseDto getWalletBalance(Long userId);

    boolean walletExists(Long userId);
}
