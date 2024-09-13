package com.jack.userservice.outbox;

public class OutboxMapper {

    // Map OutboxDTO to Outbox entity
    public static Outbox mapToEntity(OutboxDTO dto) {
        return new Outbox(
                dto.getId(),
                dto.getAggregateId(),
                dto.getAggregateType(),
                dto.getPayload(),
                dto.getCreatedAt(),
                dto.isProcessed(),
                dto.getProcessedAt()
        );
    }

    // Map Outbox entity to OutboxDTO
    public static OutboxDTO mapToDTO(Outbox entity) {
        return new OutboxDTO(
                entity.getId(),
                entity.getAggregateId(),
                entity.getAggregateType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.isProcessed(),
                entity.getProcessedAt()
        );
    }
}
