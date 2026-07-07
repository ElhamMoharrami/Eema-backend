package com.eema.backend.mapper;

import com.eema.backend.dto.response.UserDto;
import com.eema.backend.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User entity and UserDto.
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserDto.
     *
     * @param user the user entity
     * @return user DTO
     */
    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        );
    }
}