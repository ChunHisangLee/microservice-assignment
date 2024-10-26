package com.jack.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private boolean success; // Indicates if the operation was successful
    private T data;         // The data object (e.g., WalletResponseDto)
    private String error;   // Error message if the operation failed
}
