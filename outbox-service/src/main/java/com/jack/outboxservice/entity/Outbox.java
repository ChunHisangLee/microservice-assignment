package com.jack.outboxservice.entity;

import com.jack.common.constants.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox", indexes = {
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String eventType;
    private String payload;
    private String routingKey;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    private EventStatus status;
}
