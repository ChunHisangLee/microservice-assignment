package com.jack.common.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponseDto {
    public enum TransactionType {
        BUY,
        SELL
    }

    private Long id;
    private Long userId;
    private BigDecimal btcAmount;
    private BigDecimal usdAmount;
    private TransactionType transactionType;
    private LocalDateTime transactionTime;

    private BigDecimal usdBalanceBefore;
    private BigDecimal btcBalanceBefore;
    private BigDecimal usdBalanceAfter;
    private BigDecimal btcBalanceAfter;
}
