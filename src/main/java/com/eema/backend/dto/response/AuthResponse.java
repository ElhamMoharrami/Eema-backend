package com.eema.backend.dto.response;

import lombok.Builder;

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