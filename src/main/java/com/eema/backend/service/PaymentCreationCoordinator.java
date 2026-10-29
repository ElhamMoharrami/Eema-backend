package com.eema.backend.service;

import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCreationCoordinator {
    private static final Duration PROVIDER_IDEMPOTENCY_WINDOW = Duration.ofHours(1);
    private final BookingRepository bookingRepository;
    private final BookingFinancials financials;

    @Transactional
    public Claim claim(UUID bookingId) {
        Booking booking = locked(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) throw new BadRequestException("Only pending bookings can be paid");
        if (booking.getPaymentCreationStatus() == PaymentCreationStatus.CREATED) {
            return Claim.created(response(booking));
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (booking.getPaymentCreationStatus() == PaymentCreationStatus.CREATING) {
            if (booking.getPaymentCreationStartedAt() == null
                    || booking.getPaymentCreationStartedAt().plus(PROVIDER_IDEMPOTENCY_WINDOW).isBefore(now)) {
                throw new ConflictException("Payment creation requires manual reconciliation before retry");
            }
            return Claim.execute(bookingId, financials.money(booking.getVenuePriceEur()), booking.getPaymentIdempotencyKey());
        }
        booking.setPaymentCreationStatus(PaymentCreationStatus.CREATING);
        booking.setPaymentIdempotencyKey(UUID.randomUUID());
        booking.setPaymentCreationStartedAt(now);
        return Claim.execute(bookingId, financials.money(booking.getVenuePriceEur()), booking.getPaymentIdempotencyKey());
    }

    @Transactional
    public CheckoutResponse complete(Claim claim, MolliePaymentResponse payment) {
        Booking booking = locked(claim.bookingId());
        if (booking.getPaymentCreationStatus() == PaymentCreationStatus.CREATED) return response(booking);
        if (!claim.idempotencyKey().equals(booking.getPaymentIdempotencyKey())) throw new ConflictException("Payment creation claim changed");
        if (payment.id() == null || payment.id().isBlank() || payment.links() == null
                || payment.links().checkout() == null || payment.links().checkout().href() == null) {
            throw new IllegalStateException("Mollie returned an incomplete payment");
        }
        if (payment.amount() == null || !"EUR".equalsIgnoreCase(payment.amount().currency())
                || financials.money(new BigDecimal(payment.amount().value())).compareTo(claim.amount()) != 0
                || payment.metadata() == null
                || !claim.bookingId().toString().equals(payment.metadata().get("bookingId"))) {
            throw new IllegalStateException("Mollie payment response does not match booking");
        }
        booking.setMolliePaymentId(payment.id());
        booking.setMollieCheckoutUrl(payment.links().checkout().href());
        booking.setPaymentCreationStatus(PaymentCreationStatus.CREATED);
        return response(booking);
    }

    private Booking locked(UUID id) {
        return bookingRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    }

    private CheckoutResponse response(Booking b) {
        return new CheckoutResponse(b.getId(), b.getMollieCheckoutUrl(), b.getMolliePaymentId(),
                financials.money(b.getVenuePriceEur()), 300L);
    }

    public record Claim(UUID bookingId, BigDecimal amount, UUID idempotencyKey, CheckoutResponse existing) {
        static Claim execute(UUID id, BigDecimal amount, UUID key) { return new Claim(id, amount, key, null); }
        static Claim created(CheckoutResponse response) { return new Claim(response.bookingId(), response.totalAmount(), null, response); }
        public boolean alreadyCreated() { return existing != null; }
    }
}
