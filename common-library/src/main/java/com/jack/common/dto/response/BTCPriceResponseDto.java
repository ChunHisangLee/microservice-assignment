package com.jack.common.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BTCPriceResponseDto {
    private Long id;
    private BigDecimal btdPrice;
}
