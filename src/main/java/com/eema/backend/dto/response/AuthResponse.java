package com.eema.backend.dto.response;

import com.eema.backend.dto.response.UserDto;
import com.eema.backend.entity.UserRole;

import java.util.UUID;

/**
 * Response DTO for authentication responses.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {
}