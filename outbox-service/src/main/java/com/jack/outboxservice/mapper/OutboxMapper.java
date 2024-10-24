package com.jack.outboxservice.mapper;

import com.jack.common.dto.OutboxDto;
import com.jack.outboxservice.entity.Outbox;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OutboxMapper {
    // Map OutboxDto to Outbox entity
    @Mapping(target = "status", source = "dto.status")  // Direct mapping from dto.status
    @Mapping(target = "processed", source = "dto.processed")    // Map processed directly
    @Mapping(target = "routingKey", source = "dto.routingKey")
    // Map routingKey from OutboxDto
    Outbox toEntity(OutboxDto dto);

    // Map Outbox entity to OutboxDto
    @Mapping(target = "status", source = "entity.status")   // Map status from Outbox entity
    @Mapping(target = "processed", expression = "java(entity.getStatus() == com.jack.common.constants.EventStatus.PROCESSED)")
    // Determine processed
    @Mapping(target = "routingKey", source = "entity.routingKey")
    // Map routingKey from Outbox entity
    OutboxDto toDto(Outbox entity);
}
