package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.UserRole;

import java.util.UUID;

/**
 * Response DTO for user data.
 */
public record UserDto(
        UUID id,
        String email,
        UserRole role,
        String fullName,
        String phone,
        boolean active,
        boolean emailVerified
) {
}