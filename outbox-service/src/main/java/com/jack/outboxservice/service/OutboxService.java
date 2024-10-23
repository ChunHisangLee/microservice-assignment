package com.jack.outboxservice.service;

import com.jack.common.dto.OutboxDto;

import java.math.BigDecimal;
import java.util.Optional;

public interface OutboxService {
    // Method to save an Outbox entry
    OutboxDto saveOutbox(OutboxDto outboxDTO);

    // Method to process unprocessed outbox messages
    void processOutbox();

    // Method to fetch an outbox entry by ID
    Optional<OutboxDto> getOutboxById(Long id);

    // Method to process a transaction event from transaction-service
    void processTransactionEvent(Long transactionId, Long userId, BigDecimal btcAmount,BigDecimal usdAmount);
}
