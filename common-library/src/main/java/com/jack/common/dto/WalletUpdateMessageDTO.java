package com.jack.common.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateMessageDTO {
    private Long userId;
    private double usdAmount;
    private double btcAmount;
}
