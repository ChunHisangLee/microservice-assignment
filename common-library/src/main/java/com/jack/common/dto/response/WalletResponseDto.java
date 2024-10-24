package com.jack.common.dto.response;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private BigDecimal usdBalance;
    private BigDecimal btcBalance;
}
