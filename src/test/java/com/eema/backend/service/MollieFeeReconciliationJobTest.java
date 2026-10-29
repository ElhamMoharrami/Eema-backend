package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MollieFeeReconciliationJobTest {
    @Test
    void reconcilesAuthoritativeFeeOncePerPendingBooking() {
        BookingRepository repository = mock(BookingRepository.class);
        MollieFeeReportingClient client = mock(MollieFeeReportingClient.class);
        BookingService service = mock(BookingService.class);
        AppProperties properties = new AppProperties(null, null, null,
                new AppProperties.MollieProperties(null, null, null, "token", true, 1000, 1000),
                null, null, null, null, null);
        Booking booking = Booking.builder().id(UUID.randomUUID()).molliePaymentId("tr_1").build();
        when(repository.findPendingFeeReconciliation(any())).thenReturn(List.of(booking), List.of());
        when(client.findAuthoritativePaymentFee("tr_1")).thenReturn(Optional.of(new BigDecimal("0.49")));
        MollieFeeReconciliationJob job = new MollieFeeReconciliationJob(properties, repository, client, service);
        job.reconcile();
        job.reconcile();
        verify(service, times(1)).reconcileMollieFee(booking.getId(), new BigDecimal("0.49"));
    }
}
