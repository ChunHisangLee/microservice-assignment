package com.jack.transactionservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_btc_price_history_id", columnList = "btc_price_history_id")
})
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "btc_price_history_id", nullable = false, updatable = false)
    private Long btcPriceHistoryId;

    @Min(value = 0, message = "BTC amount must be greater than or equal to 0")
    @Column(nullable = false)
    private BigDecimal btcAmount;

    @Min(value = 0, message = "USD amount must be greater than or equal to 0")
    @Column(nullable = false)
    private BigDecimal usdAmount;

    @PastOrPresent(message = "Transaction time must be in the past or present")
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime transactionTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType transactionType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
