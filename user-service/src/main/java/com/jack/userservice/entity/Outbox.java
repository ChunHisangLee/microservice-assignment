package com.jack.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox", indexes = {
        @Index(name = "idx_outbox_created_at", columnList = "createdAt"),
        @Index(name = "idx_outbox_processed", columnList = "processed"),
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

    /**
     * The type of aggregate (e.g., Users, Order).
     */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /**
     * The unique identifier of the aggregate instance.
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /**
     * The type of event (e.g., USER_CREATED, USER_UPDATED).
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * The payload of the event, typically in JSON format.
     */
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Timestamp when the event was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Sequence number to maintain event order per aggregate.
     */
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    /**
     * Flag indicating whether the event has been processed/published.
     */
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    /**
     * Timestamp when the event was processed.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Unique identifier for the event to prevent duplication.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId; // UUID
}
