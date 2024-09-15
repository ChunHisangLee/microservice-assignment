package com.jack.userservice.mapper;

import com.jack.userservice.dto.UsersDTO;
import com.jack.userservice.entity.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    // Map entity to DTO, ignore fields that don't exist in Users entity
    @Mapping(target = "usdBalance", ignore = true)
    @Mapping(target = "btcBalance", ignore = true)
    UsersDTO toDTO(Users users);

    // Map DTO to entity, ignore the balances that don't exist in Users entity
    @Mapping(target = "password", ignore = true)  // Let's assume we ignore password updates
    Users toEntity(UsersDTO usersDTO);
}
