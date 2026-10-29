package com.eema.backend.dto.request;
import com.eema.backend.entity.enums.IncidentCategory;
import jakarta.validation.constraints.*;
public record CreateIncidentRequest(@NotNull IncidentCategory category,@NotBlank @Size(max=4000) @Pattern(regexp="^[^<>]*$",message="must not contain markup") String description){}
