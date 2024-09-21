package com.jack.common.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class WalletUpdateMessageDto implements Serializable {
    private Long userId;
    private BigDecimal usdAmount;
    private BigDecimal btcAmount;
}
