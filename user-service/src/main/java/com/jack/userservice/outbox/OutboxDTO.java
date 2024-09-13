package com.jack.userservice.outbox;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxDTO {

    private Long id;
    private Long aggregateId;
    private String aggregateType;
    private String payload;
    private LocalDateTime createdAt;
    private boolean processed;
    private LocalDateTime processedAt;
}
