package com.jack.outboxservice.service;

import com.jack.outboxservice.dto.OutboxDto;

import java.util.Optional;

public interface OutboxService {

    // Method to save an Outbox entry
    OutboxDto saveOutbox(OutboxDto outboxDTO);

    // Method to process unprocessed outbox messages
    void processOutbox();

    Optional<OutboxDto> getOutboxById(Long id);

}
