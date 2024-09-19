package com.jack.priceservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BTCPriceHistoryDto {
    private Long id;
    private BigDecimal price;
    private LocalDateTime timestamp;
}
