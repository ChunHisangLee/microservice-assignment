package com.jack.common.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTransactionRequestDto {
    private Long id;
    private Long userId;
    private BigDecimal btcAmount;
    private BigDecimal usdAmount;
    // Wallet balances before the transaction
    private BigDecimal usdBalanceBefore;
    private BigDecimal btcBalanceBefore;

    // Wallet balances after the transaction
    private BigDecimal usdBalanceAfter;
    private BigDecimal btcBalanceAfter;
}
