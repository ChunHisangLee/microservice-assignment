package com.jack.walletservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private Long id;
    private Long userId;
    private BigDecimal usdBalance;
    private BigDecimal btcBalance;
}
