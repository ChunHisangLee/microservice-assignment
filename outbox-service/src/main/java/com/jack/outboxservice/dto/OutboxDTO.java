package com.jack.outboxservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxDTO {
    private Long id;
    private Long aggregateId;
    private String aggregateType;
    private String payload;
    private String routingKey;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String status;  // PENDING, PROCESSED, etc.
}
