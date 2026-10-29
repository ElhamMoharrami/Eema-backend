package com.eema.backend.service;

import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.exception.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingStateMachineTest {
    private final BookingStateMachine machine = new BookingStateMachine();
    static Stream<Arguments> transitions() {
        return Arrays.stream(BookingStatus.values()).flatMap(from -> Arrays.stream(BookingStatus.values()).map(to -> Arguments.of(from,to)));
    }
    @ParameterizedTest @MethodSource("transitions")
    void completeTransitionContract(BookingStatus from, BookingStatus to) {
        Set<BookingStatus> allowed = switch(from) {
            case REQUESTED -> Set.of(BookingStatus.AWAITING_PAYMENT,BookingStatus.DECLINED,BookingStatus.EXPIRED,BookingStatus.UNAVAILABLE,BookingStatus.CANCELLED);
            case AWAITING_PAYMENT -> Set.of(BookingStatus.CONFIRMED,BookingStatus.EXPIRED,BookingStatus.CANCELLED);
            case CONFIRMED -> Set.of(BookingStatus.CANCELLED,BookingStatus.REFUNDED);
            default -> Set.of();
        };
        if(allowed.contains(to)) assertDoesNotThrow(() -> machine.requireTransition(from,to));
        else assertThrows(ConflictException.class,() -> machine.requireTransition(from,to));
    }
}
