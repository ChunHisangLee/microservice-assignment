package com.jack.priceservice.mapper;

import com.jack.priceservice.dto.BTCPriceHistoryDto;
import com.jack.priceservice.entity.BTCPriceHistory;
import org.mapstruct.Mapper;

@Mapper
public interface BTCPriceHistoryMapper {
    BTCPriceHistoryDto toDto(BTCPriceHistory btcPriceHistory);

    BTCPriceHistory toEntity(BTCPriceHistoryDto btcPriceHistoryDTO);
}
