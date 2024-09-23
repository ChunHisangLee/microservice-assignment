package com.jack.common.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OutboxRequestDto {
    private Long aggregateId;
    private String aggregateType;
    private String payload;
    private String routingKey;
    private String status;
    private String createdAt;
}
