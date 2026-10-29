package com.eema.backend.dto.request;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank String token,
                                   @NotBlank @Size(min = 8, max = 72) String newPassword) implements StrictAuthRequest {}
