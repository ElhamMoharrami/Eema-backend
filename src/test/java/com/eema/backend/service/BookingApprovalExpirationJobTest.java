package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BookingApprovalExpirationJobTest {
    @Test
    void scheduledScanExpiresApprovedUnpaidBookings() {
        BookingRepository repository = mock(BookingRepository.class);
        BookingService service = mock(BookingService.class);
        UUID id = UUID.randomUUID();
        when(repository.lockDueAwaitingPaymentBookings(any(), eq(100)))
                .thenReturn(List.of(Booking.builder().id(id).build()));
        new BookingApprovalExpirationJob(repository, service).expireUnpaidApprovals();
        verify(service).expireDueRequests(any(), eq(100));
        verify(service).expireApproval(eq(id), any());
    }
}
