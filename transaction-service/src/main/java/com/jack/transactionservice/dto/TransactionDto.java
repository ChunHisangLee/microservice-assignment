package com.jack.transactionservice.dto;

import com.jack.transactionservice.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
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
