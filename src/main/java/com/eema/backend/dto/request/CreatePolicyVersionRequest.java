package com.eema.backend.dto.request;
import com.eema.backend.entity.enums.PolicyType;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
public record CreatePolicyVersionRequest(@NotNull PolicyType policyType,@NotBlank @Size(max=50) String version,
 @NotBlank @Size(max=200) String title,@NotNull OffsetDateTime effectiveAt,@NotBlank @Size(max=200000) String content){}
