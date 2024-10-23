package com.jack.outboxservice.entity;

import com.jack.common.constants.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "outbox", indexes = {
        @Index(name = "idx_outbox_created_at", columnList = "createdAt"),
        @Index(name = "idx_outbox_processed", columnList = "status"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregateType, aggregateId, sequenceNumber")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Enumerated(EnumType.STRING) // Use Enum for status
    @Column(name = "status", nullable = false)
    private EventStatus status = EventStatus.PENDING; // Default to PENDING

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId; // UUID
}
