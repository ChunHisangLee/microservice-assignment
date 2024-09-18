package com.jack.priceservice.mapper;

import com.jack.priceservice.dto.BTCPriceHistoryDto;
import com.jack.priceservice.entity.BTCPriceHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BTCPriceHistoryMapper {

    BTCPriceHistoryMapper INSTANCE = Mappers.getMapper(BTCPriceHistoryMapper.class);

    // Automatically map between the entity and DTO
    BTCPriceHistoryDto toDto(BTCPriceHistory btcPriceHistory);

    BTCPriceHistory toEntity(BTCPriceHistoryDto btcPriceHistoryDTO);
}
