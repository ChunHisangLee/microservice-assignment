package com.jack.userservice.mapper;

import com.jack.userservice.dto.UsersDTO;
import com.jack.userservice.entity.Users;
import org.springframework.stereotype.Component;

@Component
public class UsersMapper {

    public UsersDTO toDto(Users user) {
        return UsersDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public Users toEntity(UsersDTO usersDTO) {
        Users user = new Users();
        user.setId(usersDTO.getId());
        user.setName(usersDTO.getName());
        user.setEmail(usersDTO.getEmail());
        return user;
    }
}
