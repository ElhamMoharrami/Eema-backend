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
        when(mollie.createFullRefund(anyString(), eq(bookingId), any(), any()))
                .thenThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "", null, null, null));
        assertThrows(BadRequestException.class, () -> service.requestFullRefund(bookingId));
        verify(coordinator).markRejected(bookingId);
    }
}
