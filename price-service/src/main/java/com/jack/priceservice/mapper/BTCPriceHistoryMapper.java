package com.jack.priceservice.mapper;

import com.jack.priceservice.dto.BTCPriceHistoryDto;
import com.jack.priceservice.entity.BTCPriceHistory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class BTCPriceHistoryMapper {

    public BTCPriceHistoryDto toDto(BTCPriceHistory btcPriceHistory) {
        return BTCPriceHistoryDto.builder()
                .id(btcPriceHistory.getId())
                .price(btcPriceHistory.getPrice())
                .timestamp(btcPriceHistory.getTimestamp())
                .build();
    }

    public BTCPriceHistory toEntity(BTCPriceHistoryDto btcPriceHistoryDTO) {
        return BTCPriceHistory.builder()
                .price(btcPriceHistoryDTO.getPrice())
                .timestamp(Optional.ofNullable(btcPriceHistoryDTO.getTimestamp()).orElse(LocalDateTime.now()))
                .build();
    }
}
