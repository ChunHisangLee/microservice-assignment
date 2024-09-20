package com.jack.common.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateTransactionRequestDto {
    private Long userId;
    private double btcAmount;

    private double usdBalanceBefore;
    private double btcBalanceBefore;

    private double usdBalanceAfter;
    private double btcBalanceAfter;

}
