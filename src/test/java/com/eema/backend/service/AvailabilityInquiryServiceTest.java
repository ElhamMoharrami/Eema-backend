package com.eema.backend.service;

import com.eema.backend.config.AvailabilityInquiryProperties;
import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.eema.backend.event.AvailabilityInquiryLifecycleEvent;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityInquiryServiceTest {
    @Mock AvailabilityInquiryRepository inquiries;
    @Mock VenueRepository venues;
    @Mock BookingRepository bookings;
    @Mock CurrentUserService currentUserService;
    @Mock VenueAvailabilityService availabilityService;
    @Mock BookingService bookingService;
    @Mock ApplicationEventPublisher eventPublisher;
    AvailabilityInquiryService service;

    UUID venueId;
    UUID scoutId;
    UUID ownerId;
    User scout;
    User owner;
    Venue venue;
    LocalDate futureDate;

    @BeforeEach
    void setUp() {
        service = new AvailabilityInquiryService(inquiries, venues, bookings, currentUserService,
                availabilityService, bookingService, new AvailabilityInquiryProperties(48, 60000), eventPublisher);
        venueId = UUID.randomUUID(); scoutId = UUID.randomUUID(); ownerId = UUID.randomUUID();
        scout = User.builder().id(scoutId).email("scout@example.com").fullName("Scout").role(UserRole.SCOUT).build();
        owner = User.builder().id(ownerId).email("owner@example.com").fullName("Owner").role(UserRole.OWNER).build();
        venue = Venue.builder().id(venueId).owner(owner).name("Studio").timezone("Europe/Amsterdam").status(VenueStatus.ACTIVE).build();
        futureDate = LocalDate.now(ZoneId.of("Europe/Amsterdam")).plusDays(3);
    }

    @Test
    void inquiryIsAllowedForUnavailableInterval() {
        when(currentUserService.getCurrentUser()).thenReturn(scout);
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(availabilityService.getAvailableIntervals(venueId, futureDate)).thenReturn(List.of());
        when(inquiries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new CreateAvailabilityInquiryRequest(
                venueId, futureDate, time(7), time(9), "Could the studio open early?"));

        assertEquals(AvailabilityInquiryStatus.OPEN, result.status());
        verify(inquiries).save(any(AvailabilityInquiry.class));
        verify(eventPublisher).publishEvent(new AvailabilityInquiryLifecycleEvent(
                result.id(), AvailabilityInquiryLifecycleEvent.Type.CREATED));
    }

    @Test
    void inquiryIsRejectedWhenNormalAvailabilityAlreadyContainsInterval() {
        when(currentUserService.getCurrentUser()).thenReturn(scout);
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(availabilityService.getAvailableIntervals(venueId, futureDate))
                .thenReturn(List.of(new com.eema.backend.dto.response.AvailabilityIntervalDto(time(8), time(18))));

        assertThrows(BadRequestException.class, () -> service.create(new CreateAvailabilityInquiryRequest(
                venueId, futureDate, time(9), time(11), "Already available")));
        verifyNoInteractions(inquiries);
    }

    @Test
    void unauthorizedOwnerCannotRespond() {
        AvailabilityInquiry inquiry = openInquiry();
        User otherOwner = User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).build();
        when(currentUserService.getCurrentUser()).thenReturn(otherOwner);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));

        assertThrows(ForbiddenException.class, () -> service.decline(inquiry.getId()));
        assertEquals(AvailabilityInquiryStatus.OPEN, inquiry.getStatus());
    }

    @Test
    void ownerDeclineEmitsOneLifecycleEventAndDuplicateIsRejected() {
        AvailabilityInquiry inquiry = openInquiry();
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));

        service.decline(inquiry.getId());
        assertThrows(ConflictException.class, () -> service.decline(inquiry.getId()));

        verify(eventPublisher, times(1)).publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.DECLINED));
    }

    @Test
    void ownerCannotOfferIntervalConflictingWithBooking() {
        AvailabilityInquiry inquiry = openInquiry();
        Booking conflict = Booking.builder().startTime(time(10)).endTime(time(12)).build();
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(bookings.findBlockingBookings(venueId, futureDate)).thenReturn(List.of(conflict));

        assertThrows(ConflictException.class, () -> service.offer(inquiry.getId(),
                new OfferAvailabilityInquiryRequest(futureDate, time(11), time(13))));
        assertEquals(AvailabilityInquiryStatus.OPEN, inquiry.getStatus());
    }

    @Test
    void offerExpiresAndCannotBeAccepted() {
        AvailabilityInquiry inquiry = offeredInquiry();
        inquiry.setOfferExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(currentUserService.getCurrentUser()).thenReturn(scout);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));

        assertThrows(ConflictException.class, () -> service.accept(inquiry.getId()));
        assertEquals(AvailabilityInquiryStatus.OFFERED, inquiry.getStatus());
        verifyNoInteractions(bookingService);
    }

    @Test
    void onlyInquiryScoutCanAcceptOffer() {
        AvailabilityInquiry inquiry = offeredInquiry();
        User anotherScout = User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build();
        when(currentUserService.getCurrentUser()).thenReturn(anotherScout);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));

        assertThrows(ForbiddenException.class, () -> service.accept(inquiry.getId()));
        verifyNoInteractions(bookingService);
    }

    @Test
    void ownOfferCreatesApprovedBookingAndDoesNotChangeWeeklyAvailability() {
        AvailabilityInquiry inquiry = offeredInquiry();
        UUID bookingId = UUID.randomUUID();
        BookingDto result = bookingDto(bookingId);
        when(currentUserService.getCurrentUser()).thenReturn(scout);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));
        when(bookings.findBlockingBookings(venueId, futureDate)).thenReturn(List.of());
        when(bookingService.createApprovedExceptionBooking(venueId, scoutId, futureDate, time(7), time(9), inquiry.getMessage()))
                .thenReturn(result);

        assertSame(result, service.accept(inquiry.getId()));
        assertEquals(AvailabilityInquiryStatus.ACCEPTED, inquiry.getStatus());
        assertEquals(bookingId, inquiry.getAcceptedBookingId());
        verifyNoInteractions(availabilityService);
    }

    @Test
    void offeringOneOffSlotDoesNotModifyWeeklyAvailability() {
        AvailabilityInquiry inquiry = openInquiry();
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(inquiries.findByIdForUpdate(inquiry.getId())).thenReturn(Optional.of(inquiry));
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(bookings.findBlockingBookings(venueId, futureDate)).thenReturn(List.of());

        var result = service.offer(inquiry.getId(), new OfferAvailabilityInquiryRequest(futureDate, time(7), time(9)));

        assertEquals(AvailabilityInquiryStatus.OFFERED, result.status());
        assertEquals(time(7), result.offeredStartTime());
        assertThrows(ConflictException.class, () -> service.offer(inquiry.getId(),
                new OfferAvailabilityInquiryRequest(futureDate, time(7), time(9))));
        verify(eventPublisher).publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.OFFERED));
        verifyNoInteractions(availabilityService);
    }

    @Test
    void scheduledExpiryReleasesOutstandingOffers() {
        AvailabilityInquiry inquiry = offeredInquiry();
        OffsetDateTime now = OffsetDateTime.now();
        when(inquiries.lockDueOffers(now, 100)).thenReturn(List.of(inquiry));

        assertEquals(1, service.expireOffers(now));
        assertEquals(AvailabilityInquiryStatus.EXPIRED, inquiry.getStatus());
        assertEquals(now, inquiry.getExpiredAt());
        verify(eventPublisher).publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.OFFER_EXPIRED));
    }

    private AvailabilityInquiry openInquiry() {
        return AvailabilityInquiry.builder().id(UUID.randomUUID()).venueId(venueId).scoutId(scoutId)
                .requestedDate(futureDate).requestedStartTime(time(7)).requestedEndTime(time(9))
                .message("Could this work?").status(AvailabilityInquiryStatus.OPEN).build();
    }

    private AvailabilityInquiry offeredInquiry() {
        AvailabilityInquiry inquiry = openInquiry();
        inquiry.setStatus(AvailabilityInquiryStatus.OFFERED);
        inquiry.setOfferedDate(futureDate); inquiry.setOfferedStartTime(time(7)); inquiry.setOfferedEndTime(time(9));
        inquiry.setOfferExpiresAt(OffsetDateTime.now().plusHours(2));
        return inquiry;
    }

    private LocalTime time(int hour) { return LocalTime.of(hour, 0); }

    private BookingDto bookingDto(UUID id) {
        return new BookingDto(id, venueId, scoutId, BookingStatus.AWAITING_PAYMENT,
                futureDate, time(7), time(9), "Could this work?",
                null, null, null, null, null, null, null, null, null, false, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }
}
