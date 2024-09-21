package com.jack.transactionservice.mapper;

import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @Mappings({
            @Mapping(source = "transaction.id", target = "id"),
            @Mapping(source = "transaction.userId", target = "userId"),
            @Mapping(source = "transaction.btcPriceHistoryId", target = "btcPriceHistoryId"),
            @Mapping(source = "transaction.btcAmount", target = "btcAmount"),
            @Mapping(source = "transaction.transactionTime", target = "transactionTime"),
            @Mapping(source = "transaction.transactionType", target = "transactionType"),
            @Mapping(source = "usdBalanceBefore", target = "usdBalanceBefore"),
            @Mapping(source = "btcBalanceBefore", target = "btcBalanceBefore"),
            @Mapping(source = "usdBalanceAfter", target = "usdBalanceAfter"),
            @Mapping(source = "btcBalanceAfter", target = "btcBalanceAfter")
    })
    TransactionDto toDto(Transaction transaction, BigDecimal usdBalanceBefore, BigDecimal btcBalanceBefore, BigDecimal usdBalanceAfter, BigDecimal btcBalanceAfter);

    @Mappings({
            @Mapping(source = "transactionDto.id", target = "id"),
            @Mapping(source = "transactionDto.userId", target = "userId"),
            @Mapping(source = "transactionDto.btcPriceHistoryId", target = "btcPriceHistoryId"),
            @Mapping(source = "transactionDto.btcAmount", target = "btcAmount"),
            @Mapping(source = "transactionDto.transactionTime", target = "transactionTime"),
            @Mapping(source = "transactionDto.transactionType", target = "transactionType")
            // Note: You may need to handle complex mappings or data sources for balances separately.
    })
    Transaction toEntity(TransactionDto transactionDto);
}
