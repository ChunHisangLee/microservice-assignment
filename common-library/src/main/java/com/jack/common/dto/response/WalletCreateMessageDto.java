package com.jack.common.dto.response;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class WalletCreateMessageDto implements Serializable {
    private Long userId;
    private BigDecimal initialBalance;
}
