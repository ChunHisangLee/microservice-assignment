package com.jack.outboxservice.dto;

import com.jack.common.constants.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OutboxDto {
    private Long id;

    private String eventType;
    private String payload;
    private String routingKey;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    private EventStatus status;
}
