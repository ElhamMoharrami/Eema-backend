package com.eema.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access token.
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}