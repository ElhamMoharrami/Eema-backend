package com.eema.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarkOwnerPayoutPaidRequest(
        @NotBlank @Size(max = 200) String externalTransferReference
) {}
