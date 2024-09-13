package com.jack.walletservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequestDTO {
    private Long userId;
    private double usdAmount;
    private double btcAmount;
}
