package com.jack.walletservice.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreationMessage implements Serializable {

    private Long userId;
    private Double initialBalance;
}
