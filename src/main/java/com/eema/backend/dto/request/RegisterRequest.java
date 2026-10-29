package com.eema.backend.dto.request;

import com.eema.backend.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Size(min = 2, max = 120)
        String fullName,

        @NotNull
        UserRole role

) implements StrictAuthRequest {

    public RegisterRequest {
        email = EmailNormalizer.normalize(email);
        fullName = fullName == null ? null : fullName.trim();
    }
}
