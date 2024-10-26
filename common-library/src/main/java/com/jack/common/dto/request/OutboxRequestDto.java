package com.jack.common.dto.request;

import com.jack.common.constants.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;                      // The unique identifier for the outbox entry
    private String aggregateType;         // The type of aggregate (e.g., Users, Order)
    private String aggregateId;           // The unique identifier of the aggregate instance
    private String eventType;             // The type of event (e.g., USER_CREATED, USER_UPDATED)
    private String payload;               // The payload of the event, typically in JSON format
    private LocalDateTime createdAt;      // Timestamp when the event was created
    private Long sequenceNumber;          // Sequence number to maintain event order per aggregate
    private EventStatus status;           // Status of the event (e.g., PENDING, PROCESSED)
    private boolean processed;            // Flag indicating whether the event has been processed
    private LocalDateTime processedAt;    // Timestamp when the event was processed
    private String eventId;               // Unique identifier for the event to prevent duplication
    private String routingKey;             // Routing key for the outbox event
}
