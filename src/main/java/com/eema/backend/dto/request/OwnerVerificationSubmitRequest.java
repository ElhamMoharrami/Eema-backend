package com.eema.backend.dto.request;
import jakarta.validation.constraints.Size;
public record OwnerVerificationSubmitRequest(@Size(max=1000) String note) {}
