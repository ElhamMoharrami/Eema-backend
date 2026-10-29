package com.eema.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user login.
 */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password

) implements StrictAuthRequest {

    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }
}
