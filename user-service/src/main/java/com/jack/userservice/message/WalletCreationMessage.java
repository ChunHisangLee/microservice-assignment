package com.jack.userservice.message;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletCreationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Initial balance cannot be null")
    @Min(value = 0, message = "Initial balance cannot be negative")
    private Double initialBalance;
}
