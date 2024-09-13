package com.jack.walletservice.mapper;

import com.jack.walletservice.dto.WalletDTO;
import com.jack.walletservice.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public WalletDTO toDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletDTO.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .usdBalance(wallet.getUsdBalance())
                .btcBalance(wallet.getBtcBalance())
                .build();
    }

    public Wallet toEntity(WalletDTO walletDTO) {
        if (walletDTO == null) {
            return null;
        }
        return Wallet.builder()
                .id(walletDTO.getId())
                .userId(walletDTO.getUserId())
                .usdBalance(walletDTO.getUsdBalance())
                .btcBalance(walletDTO.getBtcBalance())
                .build();
    }
}
