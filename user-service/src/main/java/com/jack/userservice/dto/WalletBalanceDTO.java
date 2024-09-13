package com.jack.userservice.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private double usdBalance;
    private double btcBalance;
}
