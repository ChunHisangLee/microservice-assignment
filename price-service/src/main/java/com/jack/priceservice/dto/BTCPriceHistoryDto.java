package com.jack.priceservice.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BTCPriceHistoryDto {
    private Long id;
    private BigDecimal price;
    private LocalDateTime timestamp;
}
