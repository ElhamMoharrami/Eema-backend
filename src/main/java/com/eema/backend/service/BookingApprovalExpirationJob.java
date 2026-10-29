package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class BookingApprovalExpirationJob {
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final java.time.Clock clock;
    BookingApprovalExpirationJob(BookingRepository repository, BookingService service) {
        this(repository, service, java.time.Clock.systemUTC());
    }

    @Scheduled(fixedDelayString = "${app.booking-workflow.expiration-scan-delay-ms:60000}")
    public void expireUnpaidApprovals() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        bookingService.expireDueRequests(now, 100);
        for (Booking booking : bookingRepository.lockDueAwaitingPaymentBookings(now, 100)) {
            bookingService.expireApproval(booking.getId(), now);
        }
    }
}
