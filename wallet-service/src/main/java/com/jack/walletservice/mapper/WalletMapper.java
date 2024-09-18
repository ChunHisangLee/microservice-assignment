package com.jack.walletservice.mapper;

import com.jack.walletservice.dto.WalletDto;
import com.jack.walletservice.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public WalletDto toDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .usdBalance(wallet.getUsdBalance())
                .btcBalance(wallet.getBtcBalance())
                .build();
    }

    public Wallet toEntity(WalletDto walletDTO) {
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
