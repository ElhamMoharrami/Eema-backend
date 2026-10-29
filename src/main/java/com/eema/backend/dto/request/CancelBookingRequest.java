package com.eema.backend.dto.request;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 1000) String reason,
        @Size(max = 2048) String previewToken
) {
}
