package com.jack.walletservice.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private double usdBalance;
    private double btcBalance;
}
