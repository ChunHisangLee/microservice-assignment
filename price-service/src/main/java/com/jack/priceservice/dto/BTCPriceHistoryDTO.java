package com.jack.priceservice.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BTCPriceHistoryDTO {
    private Long id;
    private BigDecimal price;
    private LocalDateTime timestamp;
}
