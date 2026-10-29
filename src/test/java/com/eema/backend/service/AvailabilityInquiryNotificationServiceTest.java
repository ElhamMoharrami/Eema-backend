package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.AvailabilityInquiry;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.AvailabilityInquiryStatus;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.event.AvailabilityInquiryLifecycleEvent;
import com.eema.backend.repository.AvailabilityInquiryRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AvailabilityInquiryNotificationServiceTest {
    private final AvailabilityInquiryRepository inquiries = mock(AvailabilityInquiryRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final UUID inquiryId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID scoutId = UUID.randomUUID();
    private AvailabilityInquiry inquiry;
    private AvailabilityInquiryNotificationService service;

    @BeforeEach
    void setUp() {
        User scout = User.builder().id(scoutId).fullName("Scout Name").email("scout@example.test")
                .phone("+31 6 123").role(UserRole.SCOUT).build();
        User owner = User.builder().id(UUID.randomUUID()).fullName("Owner Name").email("owner@example.test")
                .role(UserRole.OWNER).build();
        Venue venue = Venue.builder().id(venueId).name("Canal Studio").timezone("Europe/Amsterdam").owner(owner).build();
        inquiry = AvailabilityInquiry.builder().id(inquiryId).venueId(venueId).scoutId(scoutId)
                .requestedDate(LocalDate.of(2026, 8, 25)).requestedStartTime(LocalTime.of(7, 0))
                .requestedEndTime(LocalTime.of(9, 0)).message("Could you open early?")
                .status(AvailabilityInquiryStatus.OPEN).build();
        AppProperties properties = new AppProperties(null, null, null, null, null, null, null,
                "https://app.eema.test/", null);
        service = new AvailabilityInquiryNotificationService(inquiries, venues, users, email, properties);
        when(inquiries.findById(inquiryId)).thenReturn(Optional.of(inquiry));
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(users.findById(scoutId)).thenReturn(Optional.of(scout));
    }

    @Test
    void ownerReceivesNewInquiryWithExistingOwnerRoute() {
        service.sendLifecycle(inquiryId, AvailabilityInquiryLifecycleEvent.Type.CREATED);

        verify(email).send(eq("owner@example.test"), eq("New availability inquiry"),
                contains("Could you open early?"), contains("https://app.eema.test/owner/inquiries"));
    }

    @Test
    void scoutReceivesOfferWithDeadlineAndExistingScoutRoute() {
        inquiry.setStatus(AvailabilityInquiryStatus.OFFERED);
        inquiry.setOfferedDate(LocalDate.of(2026, 8, 26));
        inquiry.setOfferedStartTime(LocalTime.of(10, 0));
        inquiry.setOfferedEndTime(LocalTime.of(12, 0));
        inquiry.setOfferExpiresAt(OffsetDateTime.parse("2026-08-25T18:00:00+02:00"));

        service.sendLifecycle(inquiryId, AvailabilityInquiryLifecycleEvent.Type.OFFERED);

        verify(email).send(eq("scout@example.test"), eq("The owner offered you a time"),
                contains("25 August 2026 at 18:00 CEST"), contains("https://app.eema.test/scout/inquiries"));
    }

    @Test
    void scoutReceivesDeclineAndOfferExpiry() {
        inquiry.setStatus(AvailabilityInquiryStatus.DECLINED);
        service.sendLifecycle(inquiryId, AvailabilityInquiryLifecycleEvent.Type.DECLINED);
        verify(email).send(eq("scout@example.test"), eq("Your availability inquiry was declined"), anyString(), anyString());

        clearInvocations(email);
        inquiry.setStatus(AvailabilityInquiryStatus.EXPIRED);
        inquiry.setOfferedDate(LocalDate.of(2026, 8, 26));
        inquiry.setOfferedStartTime(LocalTime.of(10, 0));
        inquiry.setOfferedEndTime(LocalTime.of(12, 0));
        service.sendLifecycle(inquiryId, AvailabilityInquiryLifecycleEvent.Type.OFFER_EXPIRED);
        verify(email).send(eq("scout@example.test"), eq("Your availability offer expired"), anyString(), anyString());
    }

    @Test
    void providerFailureDoesNotEscapeNotificationBoundary() {
        doThrow(new EmailService.DeliveryFailure("QA_FAILURE",false))
                .when(email).send(anyString(), anyString(), anyString(), anyString());
        assertDoesNotThrow(() -> service.sendLifecycle(inquiryId, AvailabilityInquiryLifecycleEvent.Type.CREATED));
    }
}
