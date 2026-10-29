package com.eema.backend.event;

import java.util.UUID;

public record BookingLifecycleEvent(UUID bookingId, Type type) {
    public enum Type {
        REQUEST_SUBMITTED,
        REQUEST_ACCEPTED,
        REQUEST_DECLINED,
        REQUEST_EXPIRED,
        REQUEST_BECAME_UNAVAILABLE,
        APPROVAL_EXPIRED
    }
}
