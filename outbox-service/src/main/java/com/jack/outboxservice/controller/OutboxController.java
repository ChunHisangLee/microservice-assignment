package com.jack.outboxservice.controller;

import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.service.OutboxService;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/outbox")
@Log4j2
@Validated
public class OutboxController {
    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    // Endpoint to handle transaction event requests from transaction-service
    @PostMapping("/transactions/events")
    public ResponseEntity<Void> sendTransactionEvent(
            @RequestParam("transactionId") Long transactionId,
            @RequestParam("userId") Long userId,
            @RequestParam("btcAmount") BigDecimal btcAmount,
            @RequestParam("usdAmount") BigDecimal usdAmount) {
        try {
            outboxService.processTransactionEvent(transactionId, userId, btcAmount, usdAmount);
            log.info("Successfully processed transaction event: transactionId={}, userId={}, btcAmount={}, usdAmount={}",
                    transactionId, userId, btcAmount, usdAmount);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            log.error("Error processing transaction event: transactionId={}, userId={}, btcAmount={}, usdAmount={}, error={}",
                    transactionId, userId, btcAmount, usdAmount, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing transaction event", e);
        }
    }

    // Existing endpoints
    @PostMapping
    public ResponseEntity<OutboxDto> createOutbox(@Valid @RequestBody OutboxDto outboxDTO) {
        OutboxDto savedOutbox = outboxService.saveOutbox(outboxDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedOutbox);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutboxDto> getOutboxById(@PathVariable Long id) {
        OutboxDto outbox = outboxService.getOutboxById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox entry not found"));
        return ResponseEntity.ok(outbox);
    }
}
