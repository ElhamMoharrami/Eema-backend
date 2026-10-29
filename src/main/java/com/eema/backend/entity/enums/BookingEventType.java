package com.eema.backend.entity.enums;

/**
 * Booking event type enum.
 */
public enum BookingEventType {
    BOOKING_CREATED,
    PAYMENT_STARTED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    PAYMENT_RECONCILED,

    OWNER_CONFIRMED,
    OWNER_REJECTED,

    SCOUT_CANCELLED,
    OWNER_CANCELLED,

    BOOKING_COMPLETED
}