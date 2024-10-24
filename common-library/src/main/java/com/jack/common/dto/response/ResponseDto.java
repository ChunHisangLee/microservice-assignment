package com.jack.common.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDto<T> {
    private boolean success; // Indicates if the operation was successful
    private T data;         // The data object (e.g., WalletResponseDto)
    private String error;   // Error message if the operation failed
}
