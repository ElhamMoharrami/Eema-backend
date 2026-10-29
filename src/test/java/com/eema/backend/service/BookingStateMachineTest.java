package com.eema.backend.service;

import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingStateMachineTest {
    private final BookingStateMachine machine = new BookingStateMachine();

    @Test
    void permitsDefinedTransitions() {
        assertDoesNotThrow(() -> machine.requireTransition(BookingStatus.PENDING, BookingStatus.PAID));
        assertDoesNotThrow(() -> machine.requireTransition(BookingStatus.PENDING, BookingStatus.CANCELLED));
        assertDoesNotThrow(() -> machine.requireTransition(BookingStatus.PAID, BookingStatus.CONFIRMED));
        assertDoesNotThrow(() -> machine.requireTransition(BookingStatus.PAID, BookingStatus.REFUNDED));
        assertDoesNotThrow(() -> machine.requireTransition(BookingStatus.CONFIRMED, BookingStatus.REFUNDED));
    }

    @Test
    void rejectsUserSkippingPaymentAndTerminalMutations() {
        assertThrows(ConflictException.class,
                () -> machine.requireTransition(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        assertThrows(ConflictException.class,
                () -> machine.requireTransition(BookingStatus.CONFIRMED, BookingStatus.CANCELLED));
        assertThrows(ConflictException.class,
                () -> machine.requireTransition(BookingStatus.CANCELLED, BookingStatus.PAID));
    }
}
