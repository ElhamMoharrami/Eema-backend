package com.eema.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AvailabilityInquiryExpirationJob {
    private final AvailabilityInquiryService service;

    @Scheduled(fixedDelayString = "${app.availability-inquiry.expiration-scan-delay-ms:60000}")
    public void expireOffers() { service.expireOffers(OffsetDateTime.now()); }
}
