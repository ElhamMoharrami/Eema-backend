package com.eema.backend.dto.request;
import jakarta.validation.constraints.*;
public record ResolveIncidentRequest(@NotBlank @Size(max=2000) @Pattern(regexp="^[^<>]*$",message="must not contain markup") String resolutionNote){}
