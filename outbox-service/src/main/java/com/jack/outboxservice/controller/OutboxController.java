package com.jack.outboxservice.controller;

import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.service.OutboxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/outbox")
public class OutboxController {
    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    // Endpoint to save a new outbox entry
    @PostMapping
    public ResponseEntity<OutboxDto> createOutbox(@Valid @RequestBody OutboxDto outboxDTO) {
        OutboxDto savedOutbox = outboxService.saveOutbox(outboxDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOutbox); // Use CREATED (201) status for new resource
    }

    // Endpoint to fetch an outbox entry by id
    @GetMapping("/{id}")
    public ResponseEntity<OutboxDto> getOutboxById(@PathVariable Long id) {
        OutboxDto outbox = outboxService.getOutboxById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox entry not found")); // Handle not found case
        return ResponseEntity.ok(outbox);
    }
}
