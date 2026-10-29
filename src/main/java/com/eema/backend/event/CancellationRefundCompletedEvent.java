package com.eema.backend.event;

import java.util.UUID;

public record CancellationRefundCompletedEvent(UUID bookingId) {
}
