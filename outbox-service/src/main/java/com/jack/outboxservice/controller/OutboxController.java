package com.jack.outboxservice.controller;

import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.service.OutboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outbox")
public class OutboxController {

    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    // Endpoint to save a new outbox entry
    @PostMapping
    public ResponseEntity<OutboxDto> createOutbox(@RequestBody OutboxDto outboxDTO) {
        OutboxDto savedOutbox = outboxService.saveOutbox(outboxDTO);
        return ResponseEntity.ok(savedOutbox);
    }

    // Endpoint to fetch an outbox entry by id
    @GetMapping("/{id}")
    public ResponseEntity<OutboxDto> getOutboxById(@PathVariable Long id) {
        OutboxDto outbox = outboxService.getOutboxById(id);
        return ResponseEntity.ok(outbox);
    }
}
