package com.jack.walletservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private BigDecimal usdBalance;
    private BigDecimal btcBalance;
}
