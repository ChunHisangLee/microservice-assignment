package com.jack.outboxservice.mapper;

import com.jack.common.constants.EventStatus;
import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.entity.Outbox;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OutboxMapper {
    OutboxMapper INSTANCE = Mappers.getMapper(OutboxMapper.class);

    // Map OutboxDto to Outbox entity
    @Mapping(target = "status", expression = "java(mapProcessedToEventStatus(dto.isProcessed()))")
    Outbox toEntity(OutboxDto dto);

    // Map Outbox entity to OutboxDto
    @Mapping(target = "processed", expression = "java(mapEventStatusToProcessed(entity.getStatus()))")
    OutboxDto toDto(Outbox entity);

    // Helper method to map boolean 'processed' to EventStatus
    default EventStatus mapProcessedToEventStatus(boolean processed) {
        return processed ? EventStatus.PROCESSED : EventStatus.PENDING;
    }

    // Helper method to map EventStatus to boolean 'processed'
    default boolean mapEventStatusToProcessed(EventStatus status) {
        return status == EventStatus.PROCESSED;
    }
}