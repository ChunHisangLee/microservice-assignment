package com.jack.userservice.mapper;

import com.jack.userservice.dto.UserResponseDto;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsersMapper {
    // Map DTO to entity, ignore the balances that don't exist in Users entity
    @Mapping(target = "password", ignore = true)
    Users toEntity(UsersDto usersDTO);

    @Mapping(target = "usdBalance", ignore = true)
    @Mapping(target = "btcBalance", ignore = true)
    UsersDto toDto(Users user);

    @Mapping(target = "token", ignore = true)
    UserResponseDto toResponseDto(Users user);
}
