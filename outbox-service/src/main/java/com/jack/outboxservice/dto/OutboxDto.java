package com.jack.outboxservice.dto;

import com.jack.common.constants.EventStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
    private boolean processed;
}
