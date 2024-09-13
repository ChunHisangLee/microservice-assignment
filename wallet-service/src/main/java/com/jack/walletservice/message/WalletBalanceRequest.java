package com.jack.walletservice.message;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceRequest {

    private Long userId;
}
