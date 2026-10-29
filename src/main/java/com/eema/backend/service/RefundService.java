package com.eema.backend.service;

import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.dto.response.MollieRefundResponse;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    private final CurrentUserService currentUserService;
    private final RefundCoordinator coordinator;
    private final MollieProviderClient mollie;
    private final BookingService bookingService;

    public BookingDto requestFullRefund(UUID bookingId) {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != UserRole.ADMIN) throw new ForbiddenException("Only administrators can perform refunds");
        RefundCoordinator.Claim claim = coordinator.claim(bookingId, user.getId(), actorName(user));
        try {
            MollieRefundResponse refund = mollie.createFullRefund(claim.paymentId(), bookingId, claim.amount(), claim.idempotencyKey());
            coordinator.recordProviderState(claim, refund);
            log.info("Mollie refund created: bookingId={}, refundId={}, status={}", bookingId, refund.id(), refund.status());
            return bookingService.getBookingForCurrentUser(bookingId);
        } catch (HttpStatusCodeException e) {
            coordinator.markRejected(bookingId);
            log.warn("Mollie rejected refund: bookingId={}, status={}", bookingId, e.getStatusCode().value());
            throw new BadRequestException("Refund could not be initiated");
        } catch (RestClientException e) {
            log.warn("Mollie refund response was ambiguous: bookingId={}, errorType={}", bookingId, e.getClass().getSimpleName());
            throw new BadRequestException("Refund creation is pending reconciliation; retry shortly");
        }
    }

    public void requestLatePaymentRefund(UUID bookingId) {
        coordinator.claimLatePayment(bookingId).ifPresent(claim -> {
            try {
                MollieRefundResponse refund = mollie.createFullRefund(
                        claim.paymentId(), bookingId, claim.amount(), claim.idempotencyKey());
                coordinator.recordProviderState(claim, refund);
                log.info("Late-payment refund created: bookingId={}, refundId={}, status={}",
                        bookingId, refund.id(), refund.status());
            } catch (HttpStatusCodeException exception) {
                coordinator.markRejected(bookingId);
                log.warn("Mollie rejected late-payment refund: bookingId={}, status={}",
                        bookingId, exception.getStatusCode().value());
                throw new BadRequestException("Late-payment refund could not be initiated");
            } catch (RestClientException exception) {
                // CREATING and the persisted idempotency key are intentionally retained.
                log.warn("Late-payment refund response was ambiguous: bookingId={}, errorType={}",
                        bookingId, exception.getClass().getSimpleName());
                throw new BadRequestException("Late-payment refund is pending reconciliation");
            }
        });
    }

    public void reconcileForPayment(String paymentId) {
        // No refund is a normal payment-webhook condition.
        bookingService.findRefundReference(paymentId).ifPresent(reference -> {
            MollieRefundResponse refund = mollie.getRefund(paymentId, reference);
            coordinator.reconcile(paymentId, refund);
        });
    }

    private String actorName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }
}
