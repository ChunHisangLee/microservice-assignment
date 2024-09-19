package com.jack.common.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutboxRequestDto {
    private Long aggregateId;
    private String aggregateType;
    private String payload;
    private String routingKey;
    private String status;
    private String createdAt;
}
