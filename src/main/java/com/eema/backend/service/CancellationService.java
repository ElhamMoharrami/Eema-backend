package com.eema.backend.service;

import com.eema.backend.dto.request.CancelBookingRequest;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.dto.response.CancellationPreviewDto;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancellationService {
    private static final Set<String> TERMINAL_UNPAID_PAYMENT_STATES =
            Set.of("canceled", "expired", "failed");

    private final CurrentUserService currentUserService;
    private final BookingRepository bookings;
    private final BookingCancellationCoordinator coordinator;
    private final RefundService refunds;
    private final MollieProviderClient mollie;
    private final BookingService bookingService;
    private final CancellationClock clock;

    public CancellationPreviewDto preview(UUID bookingId) {
        return coordinator.preview(bookingId, currentUserService.getCurrentUser(), clock.now());
    }

    public BookingDto cancel(UUID bookingId, CancelBookingRequest request) {
        User actor = currentUserService.getCurrentUser();
        CancellationPreviewDto preview = coordinator.preview(bookingId, actor, clock.now());
        Booking snapshot = bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (!preview.canCancel()) {
            if (snapshot.getStatus() == BookingStatus.CANCELLED) {
                refunds.resumeCancellationRefund(bookingId);
                return bookingService.getBookingForCurrentUser(bookingId);
            }
            throw new ConflictException("Booking cannot be cancelled in its current state");
        }

        if (snapshot.getStatus() == BookingStatus.AWAITING_PAYMENT
                && snapshot.getMolliePaymentId() != null) {
            handleOpenPayment(snapshot);
        }

        BookingCancellationCoordinator.CancellationResult result = coordinator.cancel(
                bookingId, actor, request == null ? null : request.reason(), clock.now());
        if (result.refundClaim() != null) {
            refunds.requestCancellationRefund(result.refundClaim());
        }
        return bookingService.getBookingForCurrentUser(bookingId);
    }

    private void handleOpenPayment(Booking booking) {
        try {
            MolliePaymentResponse payment = mollie.getPayment(booking.getMolliePaymentId());
            String status = payment.status() == null ? "" : payment.status().toLowerCase();
            if ("paid".equals(status)) {
                bookingService.processPaidPayment(payment);
                return;
            }
            if (!TERMINAL_UNPAID_PAYMENT_STATES.contains(status)) {
                mollie.cancelPayment(booking.getMolliePaymentId());
            }
        } catch (RestClientException exception) {
            throw new BadRequestException(
                    "Payment state could not be safely resolved; cancellation was not recorded"
            );
        }
    }
}
