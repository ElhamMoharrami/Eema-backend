package com.eema.backend.service;

import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.exception.ConflictException;
import org.springframework.stereotype.Component;

@Component
public class BookingStateMachine {

    public void requireTransition(BookingStatus current, BookingStatus target) {
        boolean valid = switch (current) {
            case REQUESTED -> target == BookingStatus.AWAITING_PAYMENT || target == BookingStatus.DECLINED
                    || target == BookingStatus.EXPIRED || target == BookingStatus.UNAVAILABLE
                    || target == BookingStatus.CANCELLED;
            case AWAITING_PAYMENT -> target == BookingStatus.CONFIRMED || target == BookingStatus.EXPIRED
                    || target == BookingStatus.CANCELLED;
            case PENDING -> target == BookingStatus.PAID || target == BookingStatus.CANCELLED;
            case PAID -> target == BookingStatus.CONFIRMED || target == BookingStatus.REFUNDED;
            case CONFIRMED -> target == BookingStatus.REFUNDED;
            case DECLINED, EXPIRED, UNAVAILABLE, CANCELLED, REFUNDED -> false;
        };

        if (!valid) {
            throw new ConflictException("Invalid booking status transition: " + current + " -> " + target);
        }
    }
}
