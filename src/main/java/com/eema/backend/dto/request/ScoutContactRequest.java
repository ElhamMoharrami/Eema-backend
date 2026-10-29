package com.eema.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScoutContactRequest(
    @NotBlank(message = "name is required")
    @Size(min = 2, max = 100, message = "name must be 2-100 characters")
    String name,
    
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 255)
    String email,
    
    @Size(max = 50)
    String phone  // optional
) {}
