package com.eema.backend.service;

import com.eema.backend.entity.enums.BookingStatus;
import java.util.EnumSet;
import java.util.Set;

/** Defines which lifecycle states reserve venue time. Cancelled and refunded bookings release it. */
public final class BookingReservationPolicy {
    private static final Set<BookingStatus> BLOCKING = EnumSet.of(
            BookingStatus.AWAITING_PAYMENT,
            BookingStatus.PENDING, BookingStatus.PAID, BookingStatus.CONFIRMED);
    private BookingReservationPolicy() {}
    public static boolean blocksAvailability(BookingStatus status) { return BLOCKING.contains(status); }
}
