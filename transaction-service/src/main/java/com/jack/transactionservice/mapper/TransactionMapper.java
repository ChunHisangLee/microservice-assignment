package com.jack.transactionservice.mapper;

import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @Mapping(source = "usdBalanceBefore", target = "usdBalanceBefore")
    @Mapping(source = "btcBalanceBefore", target = "btcBalanceBefore")
    @Mapping(source = "newUsdBalance", target = "usdBalanceAfter")
    @Mapping(source = "newBtcBalance", target = "btcBalanceAfter")
    TransactionDto toDto(Transaction transaction, BigDecimal usdBalanceBefore,
                         BigDecimal btcBalanceBefore, BigDecimal newUsdBalance,
                         BigDecimal newBtcBalance);
}
