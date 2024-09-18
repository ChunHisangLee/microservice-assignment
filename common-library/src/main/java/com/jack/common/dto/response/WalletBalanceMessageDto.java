package com.jack.common.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WalletBalanceMessageDto {
    private Long userId;
}
