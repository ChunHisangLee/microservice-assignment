package com.jack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreateMessageDTO implements Serializable {
    private Long userId;
    private Double initialBalance;
}
