package com.eema.backend.dto.request;
import com.eema.backend.entity.enums.VerificationMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record OwnerVerificationApproveRequest(@NotNull VerificationMethod verificationMethod,
                                              @Size(max=1000) String note) {}
