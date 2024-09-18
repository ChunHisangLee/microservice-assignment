package com.jack.common.dto.response;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private double usdBalance;
    private double btcBalance;
}