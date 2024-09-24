package com.jack.common.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponseDto {
    private Long userId;
    private BigDecimal usdBalance;
    private BigDecimal btcBalance;
}
