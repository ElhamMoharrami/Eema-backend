package com.eema.backend.service;

import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentCreationCoordinatorTest {
    private final BookingRepository repository = mock(BookingRepository.class);
    private final PaymentCreationCoordinator coordinator = new PaymentCreationCoordinator(repository, new BookingFinancials());

    @Test
    void concurrentClaimsReuseDurableProviderKeyAndCompletionIsIdempotent() {
        UUID id = UUID.randomUUID();
        UUID scoutId = UUID.randomUUID();
        Booking booking = Booking.builder().id(id).scoutId(scoutId).status(BookingStatus.PENDING)
                .venuePriceEur(new BigDecimal("100.00")).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        PaymentCreationCoordinator.Claim first = coordinator.claim(id, scoutId);
        PaymentCreationCoordinator.Claim concurrent = coordinator.claim(id, scoutId);
        assertEquals(first.idempotencyKey(), concurrent.idempotencyKey());
        assertEquals(PaymentCreationStatus.CREATING, booking.getPaymentCreationStatus());

        MolliePaymentResponse response = new MolliePaymentResponse("tr_one", "open",
                new MolliePaymentResponse.Amount("100.00", "EUR"),
                new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout")),
                java.util.Map.of("bookingId", id.toString()));
        assertEquals("tr_one", coordinator.complete(first, response).molliePaymentId());
        assertEquals("tr_one", coordinator.complete(concurrent, response).molliePaymentId());
        assertEquals(PaymentCreationStatus.CREATED, booking.getPaymentCreationStatus());
    }

    @Test
    void refusesUnsafeRetryOutsideMollieReplayWindow() {
        UUID id = UUID.randomUUID();
        UUID scoutId = UUID.randomUUID();
        Booking booking = Booking.builder().id(id).scoutId(scoutId).status(BookingStatus.PENDING)
                .venuePriceEur(BigDecimal.TEN).paymentCreationStatus(PaymentCreationStatus.CREATING)
                .paymentIdempotencyKey(UUID.randomUUID()).paymentCreationStartedAt(OffsetDateTime.now().minusHours(2)).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        assertThrows(ConflictException.class, () -> coordinator.claim(id, scoutId));
    }

    @Test
    void checkoutBeforeOwnerApprovalIsRejected() {
        UUID id = UUID.randomUUID();
        UUID scoutId = UUID.randomUUID();
        Booking booking = Booking.builder().id(id).scoutId(scoutId).status(BookingStatus.REQUESTED).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        assertThrows(BadRequestException.class, () -> coordinator.claim(id, scoutId));
    }

    @Test
    void checkoutAfterApprovalExpirationIsRejected() {
        UUID id = UUID.randomUUID();
        UUID scoutId = UUID.randomUUID();
        Booking booking = Booking.builder().id(id).scoutId(scoutId).status(BookingStatus.AWAITING_PAYMENT)
                .paymentExpiresAt(OffsetDateTime.now().minusMinutes(1)).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        assertThrows(ConflictException.class, () -> coordinator.claim(id, scoutId));
    }
}
