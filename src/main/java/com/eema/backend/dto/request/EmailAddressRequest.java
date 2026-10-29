package com.eema.backend.dto.request;
import jakarta.validation.constraints.*;
public record EmailAddressRequest(@NotBlank @Email @Size(max = 254) String email) {}
