package com.jack.common.dto.response;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class WalletCreateMessageDto implements Serializable {
    private Long userId;
    private Double initialBalance;
}
