package com.jack.walletservice.mapper;

import com.jack.walletservice.dto.WalletDto;
import com.jack.walletservice.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WalletMapper {
    WalletMapper INSTANCE = Mappers.getMapper(WalletMapper.class);

    // Map entity to DTO
    WalletDto toDto(Wallet wallet);

    // Map DTO to entity
    Wallet toEntity(WalletDto walletDTO);
}
