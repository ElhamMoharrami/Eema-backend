package com.eema.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}