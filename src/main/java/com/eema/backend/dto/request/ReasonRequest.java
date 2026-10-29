package com.eema.backend.dto.request;
import jakarta.validation.constraints.*;
public record ReasonRequest(@NotBlank @Size(max=1000) String reason) {}
