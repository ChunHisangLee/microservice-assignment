package com.jack.walletservice.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateMessage {

    private Long userId;
    private Double usdAmount;
    private Double btcAmount;
}
