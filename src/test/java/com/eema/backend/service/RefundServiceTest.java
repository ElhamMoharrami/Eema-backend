package com.eema.backend.service;

import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.dto.response.MollieRefundResponse;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RefundServiceTest {
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final RefundCoordinator coordinator = mock(RefundCoordinator.class);
    private final MollieProviderClient mollie = mock(MollieProviderClient.class);
    private final BookingService bookings = mock(BookingService.class);
    private final RefundService service = new RefundService(current, coordinator, mollie, bookings);

    @Test
    void onlyAdminMayRefund() {
        when(current.getCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).build());
        assertThrows(ForbiddenException.class, () -> service.requestFullRefund(UUID.randomUUID()));
        verifyNoInteractions(coordinator, mollie);
    }

    @Test
    void definitiveProviderRejectionIsRecordedAndSanitized() {
        UUID bookingId = UUID.randomUUID();
        User admin = User.builder().id(UUID.randomUUID()).email("admin@example.com").role(UserRole.ADMIN).build();
        RefundCoordinator.Claim claim = new RefundCoordinator.Claim(bookingId, "tr_1", new BigDecimal("100.00"), UUID.randomUUID());
        when(current.getCurrentUser()).thenReturn(admin);
        when(coordinator.claim(eq(bookingId), eq(admin.getId()), anyString())).thenReturn(claim);
        when(mollie.createRefund(anyString(), eq(bookingId), any(), any()))
                .thenThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "", null, null, null));
        assertThrows(BadRequestException.class, () -> service.requestFullRefund(bookingId));
        verify(coordinator).markRejected(bookingId);
    }

    @Test
    void latePaymentProviderFailureRetainsCoordinatorStateForSafeRetry() {
        UUID bookingId = UUID.randomUUID();
        RefundCoordinator.Claim claim = new RefundCoordinator.Claim(
                bookingId, "tr_late", new BigDecimal("100.00"), UUID.randomUUID());
        when(coordinator.claimLatePayment(bookingId)).thenReturn(java.util.Optional.of(claim));
        when(mollie.createRefund(anyString(), eq(bookingId), any(), eq(claim.idempotencyKey())))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));

        assertThrows(BadRequestException.class, () -> service.requestLatePaymentRefund(bookingId));
        verify(coordinator, never()).markRejected(bookingId);
    }

    @Test
    void cancellationRefundUsesExactClaimedPartialAmount() {
        UUID bookingId = UUID.randomUUID();
        RefundCoordinator.Claim claim = new RefundCoordinator.Claim(
                bookingId, "tr_partial", new BigDecimal("500.00"), UUID.randomUUID());
        MollieRefundResponse response = new MollieRefundResponse(
                "re_1", "queued", "tr_partial",
                new MolliePaymentResponse.Amount("500.00", "EUR"));
        when(mollie.createRefund("tr_partial", bookingId, new BigDecimal("500.00"), claim.idempotencyKey()))
                .thenReturn(response);

        service.requestCancellationRefund(claim);

        verify(coordinator).recordProviderState(claim, response);
    }

    @Test
    void duplicateCancellationClaimDoesNotCreateAnotherProviderRefund() {
        UUID bookingId = UUID.randomUUID();
        User scout = User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build();
        when(current.getCurrentUser()).thenReturn(scout);
        assertThrows(ForbiddenException.class, () -> service.requestFullRefund(bookingId));
        verifyNoInteractions(mollie);
    }
}
