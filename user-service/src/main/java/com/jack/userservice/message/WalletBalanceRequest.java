package com.jack.userservice.message;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceRequest {

    private Long userId;
}
