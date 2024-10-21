package com.jack.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId; // UUID

    /**
     * Timestamp when the event was processed.
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
