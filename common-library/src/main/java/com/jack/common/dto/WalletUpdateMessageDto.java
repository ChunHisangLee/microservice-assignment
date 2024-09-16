package com.jack.common.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateMessageDto {
    private Long userId;
    private double usdAmount;
    private double btcAmount;
}
