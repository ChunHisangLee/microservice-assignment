package com.jack.common.dto.response;

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
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto implements Serializable {
    public enum TransactionType {
        BUY,
        SELL
    }

    @Serial
    private static final long serialVersionUID = 1L;
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
