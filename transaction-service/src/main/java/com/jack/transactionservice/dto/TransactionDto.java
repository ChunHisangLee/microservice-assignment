package com.jack.transactionservice.dto;

import com.jack.transactionservice.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TransactionDto {
    private Long id;
    private Long userId;
    private Long btcPriceHistoryId;

    private double btcAmount;
    private LocalDateTime transactionTime;
    private TransactionType transactionType;

    private double usdBalanceBefore;
    private double btcBalanceBefore;
    private double usdBalanceAfter;
    private double btcBalanceAfter;
}
