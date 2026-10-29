package com.eema.backend.event;

import java.util.UUID;

public record AvailabilityInquiryLifecycleEvent(UUID inquiryId, Type type) {
    public enum Type {
        CREATED,
        OFFERED,
        DECLINED,
        OFFER_EXPIRED
    }
}
