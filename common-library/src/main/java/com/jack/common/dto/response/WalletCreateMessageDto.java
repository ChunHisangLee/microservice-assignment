package com.jack.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class WalletCreateMessageDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private BigDecimal usdAmount;
    private BigDecimal btcAmount;
}
