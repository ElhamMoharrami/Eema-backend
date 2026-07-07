package com.eema.backend.dto.request;

import jakarta.validation.constraints.*;

public record ScoutContactDto(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone
) {
}