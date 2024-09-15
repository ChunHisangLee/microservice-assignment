package com.jack.outboxservice.mapper;

import com.jack.common.entity.Outbox;
import com.jack.outboxservice.dto.OutboxDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OutboxMapper {
    OutboxMapper INSTANCE = Mappers.getMapper(OutboxMapper.class);

    OutboxDTO toDTO(Outbox outbox);

    Outbox toEntity(OutboxDTO outboxDTO);
}
