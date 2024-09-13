package com.jack.userservice.outbox;

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
    public ResponseEntity<OutboxDTO> createOutbox(@RequestBody OutboxDTO outboxDTO) {
        OutboxDTO savedOutbox = outboxService.saveOutbox(outboxDTO);
        return ResponseEntity.ok(savedOutbox);
    }

    // Endpoint to fetch an outbox entry by id
    @GetMapping("/{id}")
    public ResponseEntity<OutboxDTO> getOutboxById(@PathVariable Long id) {
        OutboxDTO outbox = outboxService.getOutboxById(id);
        return ResponseEntity.ok(outbox);
    }
}
