package com.jack.common.dto.response;

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
public class WalletUpdateMessageDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private BigDecimal usdAmount;
    private BigDecimal btcAmount;
}
