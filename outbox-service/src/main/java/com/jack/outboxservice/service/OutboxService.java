package com.jack.outboxservice.service;

import com.jack.outboxservice.dto.OutboxDto;

import java.util.Optional;

public interface OutboxService {

    // Method to save an Outbox entry
    OutboxDto saveOutbox(OutboxDto outboxDTO);

    // Method to process unprocessed outbox messages
    void processOutbox();

    // Method to fetch an outbox entry by ID
    Optional<OutboxDto> getOutboxById(Long id);

    // Method to process a transaction event from transaction-service
    void processTransactionEvent(Long transactionId, Long userId, double btcAmount);
}
