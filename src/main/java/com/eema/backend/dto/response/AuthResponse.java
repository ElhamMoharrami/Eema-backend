package com.eema.backend.dto.response;

import com.eema.backend.dto.response.UserDto;
import com.eema.backend.entity.UserRole;
import lombok.Builder;

import java.util.UUID;

/**
 * Response DTO for authentication responses.
 */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {
}