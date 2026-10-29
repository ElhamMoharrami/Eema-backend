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
import java.time.Clock;
import com.eema.backend.exception.PaymentDeadlineExpiredException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class PaymentCreationCoordinator {
    private static final Duration PROVIDER_IDEMPOTENCY_WINDOW = Duration.ofHours(1);
    private final BookingRepository bookingRepository;
    private final BookingFinancials financials;
    private final Clock clock;
    private final BookingService bookings;

    PaymentCreationCoordinator(BookingRepository repository, BookingFinancials financials) {
        this(repository, financials, Clock.systemUTC(), null);
    }


    @Transactional(noRollbackFor = PaymentDeadlineExpiredException.class)
    public Claim claim(UUID bookingId, UUID scoutId) {
        Booking booking = locked(bookingId);
        if (!booking.getScoutId().equals(scoutId)) throw new com.eema.backend.exception.ForbiddenException("Only the requesting scout can pay this booking");
        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT) {
            throw new BadRequestException("Only owner-approved bookings can be paid");
        }
        requireOpenWindow(booking);
        // Defense for legacy/corrupt snapshots, before either a new claim or cached checkout.
        financials.requireBookablePrice(booking.getVenuePriceEur());
        if (bookings != null) bookings.validateCommitmentTime(booking);
        if (booking.getPaymentCreationStatus() == PaymentCreationStatus.CREATED) {
            return Claim.created(response(booking));
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
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

    @Transactional(noRollbackFor = PaymentDeadlineExpiredException.class)
    public CheckoutResponse complete(Claim claim, MolliePaymentResponse payment) {
        Booking booking = locked(claim.bookingId());
        if (booking.getPaymentCreationStatus() == PaymentCreationStatus.CREATED) {
            requireOpenWindow(booking);
            return response(booking);
        }
        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT
                && booking.getStatus() != BookingStatus.EXPIRED
                && booking.getStatus() != BookingStatus.CANCELLED) {
            throw new ConflictException("Booking is no longer eligible for payment");
        }
        if (!claim.idempotencyKey().equals(booking.getPaymentIdempotencyKey())) throw new ConflictException("Payment creation claim changed");
        if (payment == null || !MollieResponseValidation.paymentId(payment.id()) || payment.links() == null
                || payment.links().checkout() == null || payment.links().checkout().href() == null) {
            throw new IllegalStateException("Mollie returned an incomplete payment");
        }
        MollieResponseValidation.checkoutUrl(payment.links().checkout().href());
        if (MollieResponseValidation.eurAmount(payment.amount()).compareTo(claim.amount()) != 0
                || payment.metadata() == null
                || !claim.bookingId().toString().equals(payment.metadata().get("bookingId"))) {
            throw new IllegalStateException("Mollie payment response does not match booking");
        }
        booking.setMolliePaymentId(payment.id());
        booking.setMollieCheckoutUrl(payment.links().checkout().href());
        booking.setPaymentCreationStatus(PaymentCreationStatus.CREATED);
        // Preserve an in-flight provider result even if inventory has expired meanwhile.
        // Its ID is needed to validate and refund a subsequent late-paid callback.
        requireOpenWindow(booking);
        return response(booking);
    }

    private void requireOpenWindow(Booking booking) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT
                || booking.getPaymentExpiresAt() == null || !booking.getPaymentExpiresAt().isAfter(now)) {
            if (booking.getStatus() == BookingStatus.AWAITING_PAYMENT) {
                if (bookings != null) bookings.expireApproval(booking.getId(), now);
                else { booking.setStatus(BookingStatus.EXPIRED); booking.setExpiredAt(now); }
            }
            throw new PaymentDeadlineExpiredException("The approved payment window has expired");
        }
    }

    private Booking locked(UUID id) {
        return bookingRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    }

    private CheckoutResponse response(Booking b) {
        long expiresIn = Math.max(0L, Duration.between(OffsetDateTime.now(clock), b.getPaymentExpiresAt()).toSeconds());
        return new CheckoutResponse(b.getId(), b.getMollieCheckoutUrl(), b.getMolliePaymentId(),
                financials.money(b.getVenuePriceEur()), expiresIn);
    }

    public record Claim(UUID bookingId, BigDecimal amount, UUID idempotencyKey, CheckoutResponse existing) {
        static Claim execute(UUID id, BigDecimal amount, UUID key) { return new Claim(id, amount, key, null); }
        static Claim created(CheckoutResponse response) { return new Claim(response.bookingId(), response.totalAmount(), null, response); }
        public boolean alreadyCreated() { return existing != null; }
    }
}
