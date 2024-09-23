package com.jack.common.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BTCPriceResponseDto {
    private Long id;
    private BigDecimal btcPrice;
}
