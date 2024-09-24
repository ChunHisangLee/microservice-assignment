package com.jack.common.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTransactionRequestDto {
    private Long userId;
    private BigDecimal btcAmount;
}
