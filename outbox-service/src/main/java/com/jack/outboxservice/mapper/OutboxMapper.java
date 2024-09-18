package com.jack.outboxservice.mapper;

import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.entity.Outbox;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OutboxMapper {
    OutboxMapper INSTANCE = Mappers.getMapper(OutboxMapper.class);

    // Map OutboxDto to Outbox entity
    @Mapping(target = "status", expression = "java(dto.isProcessed() ? EventStatus.PROCESSED : EventStatus.PENDING)")
    Outbox toEntity(OutboxDto dto);

    // Map Outbox entity to OutboxDto
    @Mapping(target = "processed", expression = "java(entity.getStatus() != EventStatus.PENDING)")
    OutboxDto toDto(Outbox entity);
}
