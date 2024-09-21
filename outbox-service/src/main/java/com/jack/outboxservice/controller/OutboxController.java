package com.jack.outboxservice.controller;

import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.service.OutboxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/outbox")
public class OutboxController {
    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    // Endpoint to handle transaction event requests from transaction-service
    @PostMapping("/sendTransactionEvent")
    public ResponseEntity<Void> sendTransactionEvent(
            @RequestParam("transactionId") Long transactionId,
            @RequestParam("userId") Long userId,
            @RequestParam("btcAmount") BigDecimal btcAmount,
            @RequestParam("usdAmount") BigDecimal usdAmount) {
        try {
            // Here you can handle the logic to create an outbox event from the transaction details
            outboxService.processTransactionEvent(transactionId, userId, btcAmount,usdAmount);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing transaction event");
        }
    }

    // Existing endpoints
    @PostMapping
    public ResponseEntity<OutboxDto> createOutbox(@Valid @RequestBody OutboxDto outboxDTO) {
        OutboxDto savedOutbox = outboxService.saveOutbox(outboxDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOutbox); // Use CREATED (201) status for new resource
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutboxDto> getOutboxById(@PathVariable Long id) {
        OutboxDto outbox = outboxService.getOutboxById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox entry not found"));
        return ResponseEntity.ok(outbox);
    }
}
