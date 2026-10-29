package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class BookingApprovalExpirationJob {
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${app.booking-workflow.expiration-scan-delay-ms:60000}")
    public void expireUnpaidApprovals() {
        OffsetDateTime now = OffsetDateTime.now();
        bookingService.expireDueRequests(now, 100);
        for (Booking booking : bookingRepository.findByStatusAndPaymentExpiresAtBefore(BookingStatus.AWAITING_PAYMENT, now)) {
            bookingService.expireApproval(booking.getId(), now);
        }
    }
}
