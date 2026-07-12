package com.eema.backend.entity.enums;

/**
 * Booking event type enum.
 */
public enum BookingEventType {
    BOOKING_CREATED,
    AYMENT_STARTED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,

    OWNER_CONFIRMED,
    OWNER_REJECTED,

    SCOUT_CANCELLED,
    OWNER_CANCELLED,

    BOOKING_COMPLETED
}