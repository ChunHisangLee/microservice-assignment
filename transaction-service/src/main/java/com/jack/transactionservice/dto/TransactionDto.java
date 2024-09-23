package com.jack.transactionservice.dto;

import com.jack.transactionservice.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    private Long id;
    private Long userId;
    private Long btcPriceHistoryId;

    private BigDecimal btcAmount;
    private BigDecimal usdAmount;
    private LocalDateTime transactionTime;
    private TransactionType transactionType;

    private BigDecimal usdBalanceBefore;
    private BigDecimal btcBalanceBefore;
    private BigDecimal usdBalanceAfter;
    private BigDecimal btcBalanceAfter;
}
