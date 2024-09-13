package com.jack.walletservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Long id;
    private Long userId;
    private double usdBalance;
    private double btcBalance;
}
