package com.jack.common.dto.response;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class WalletResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private BigDecimal usdBalance;
    private BigDecimal btcBalance;
}
