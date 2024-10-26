package com.jack.priceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BTCPriceHistoryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private BigDecimal price;
    private Instant timestamp;
}
