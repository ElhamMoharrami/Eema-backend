package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationalExceptionDto(OperationalExceptionType type,
        OperationalExceptionSeverity severity, UUID bookingId, UUID payoutId,
        UUID liabilityId, OffsetDateTime detectedAt, String reason) {}
