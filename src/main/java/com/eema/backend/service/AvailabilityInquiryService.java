package com.eema.backend.service;

import com.eema.backend.config.AvailabilityInquiryProperties;
import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.eema.backend.event.AvailabilityInquiryLifecycleEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class AvailabilityInquiryService {
    private final AvailabilityInquiryRepository inquiries;
    private final VenueRepository venues;
    private final BookingRepository bookings;
    private final CurrentUserService currentUserService;
    private final VenueAvailabilityService availabilityService;
    private final BookingService bookingService;
    private final AvailabilityInquiryProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final VenueBookingLeadTimePolicy leadTimePolicy;
    private final PolicyAcceptanceService policyAcceptances;
    private final Clock clock;
    AvailabilityInquiryService(AvailabilityInquiryRepository a,VenueRepository b,BookingRepository c,CurrentUserService d,
      VenueAvailabilityService e,BookingService f,AvailabilityInquiryProperties g,ApplicationEventPublisher h,VenueBookingLeadTimePolicy i){this(a,b,c,d,e,f,g,h,i,null,Clock.systemUTC());}

    @Transactional
    public AvailabilityInquiryDto create(CreateAvailabilityInquiryRequest request) {
        User scout = requireRole(UserRole.SCOUT);
        if(policyAcceptances!=null) policyAcceptances.requireCurrentPolicies(scout);
        Venue venue = activeVenue(request.venueId());
        validateFutureInterval(venue, request.requestedDate(), request.requestedStartTime(), request.requestedEndTime());
        leadTimePolicy.validate(venue, request.requestedDate(), request.requestedStartTime());
        boolean normallyAvailable = availabilityService.getAvailableIntervals(venue.getId(), request.requestedDate()).stream()
                .anyMatch(interval -> !request.requestedStartTime().isBefore(interval.startTime())
                        && !request.requestedEndTime().isAfter(interval.endTime()));
        if (normallyAvailable) throw new BadRequestException("This interval is available; submit a normal booking request instead");
        AvailabilityInquiry inquiry = AvailabilityInquiry.builder()
                .venueId(venue.getId()).scoutId(scout.getId())
                .requestedDate(request.requestedDate()).requestedStartTime(request.requestedStartTime())
                .requestedEndTime(request.requestedEndTime()).message(request.message().trim())
                .status(AvailabilityInquiryStatus.OPEN).build();
        inquiry = inquiries.save(inquiry);
        eventPublisher.publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.CREATED));
        return dto(inquiry, venue);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityInquiryDto> mine() {
        User scout = requireRole(UserRole.SCOUT);
        return inquiries.findByScoutIdOrderByCreatedAtDesc(scout.getId()).stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilityInquiryDto> ownedVenueInquiries() {
        User owner = requireRole(UserRole.OWNER);
        List<UUID> venueIds = venues.findByOwnerId(owner.getId()).stream().map(Venue::getId).toList();
        if (venueIds.isEmpty()) return List.of();
        return inquiries.findByVenueIdInOrderByCreatedAtDesc(venueIds).stream().map(this::dto).toList();
    }

    @Transactional
    public AvailabilityInquiryDto decline(UUID inquiryId) {
        User owner = requireRole(UserRole.OWNER);
        if(policyAcceptances!=null) policyAcceptances.requireCurrentPolicies(owner);
        AvailabilityInquiry inquiry = locked(inquiryId);
        Venue venue = ownedVenue(inquiry, owner);
        requireStatus(inquiry, AvailabilityInquiryStatus.OPEN);
        inquiry.setStatus(AvailabilityInquiryStatus.DECLINED);
        inquiry.setDeclinedAt(OffsetDateTime.now(clock));
        eventPublisher.publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.DECLINED));
        return dto(inquiry, venue);
    }

    @Transactional
    public AvailabilityInquiryDto offer(UUID inquiryId, OfferAvailabilityInquiryRequest request) {
        User owner = requireRole(UserRole.OWNER);
        if(policyAcceptances!=null) policyAcceptances.requireCurrentPolicies(owner);
        AvailabilityInquiry inquiry = locked(inquiryId);
        Venue venue = ownedVenue(inquiry, owner, true);
        requireStatus(inquiry, AvailabilityInquiryStatus.OPEN);
        if (venue.getStatus() != VenueStatus.ACTIVE) throw new ConflictException("Venue must be active to offer a time");
        validateFutureInterval(venue, request.offeredDate(), request.offeredStartTime(), request.offeredEndTime());
        leadTimePolicy.validate(venue, request.offeredDate(), request.offeredStartTime());
        ensureNoBookingConflict(venue.getId(), request.offeredDate(), request.offeredStartTime(), request.offeredEndTime());
        OffsetDateTime now = OffsetDateTime.now(clock);
        inquiry.setOfferedDate(request.offeredDate());
        inquiry.setOfferedStartTime(request.offeredStartTime());
        inquiry.setOfferedEndTime(request.offeredEndTime());
        inquiry.setOfferedAt(now);
        OffsetDateTime configuredExpiry = now.plusHours(properties.offerValidHours());
        OffsetDateTime slotStart = ZonedDateTime.of(request.offeredDate(), request.offeredStartTime(),
                ZoneId.of(venue.getTimezone())).toOffsetDateTime();
        inquiry.setOfferExpiresAt(configuredExpiry.isBefore(slotStart) ? configuredExpiry : slotStart);
        inquiry.setStatus(AvailabilityInquiryStatus.OFFERED);
        eventPublisher.publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.OFFERED));
        return dto(inquiry, venue);
    }

    @Transactional(noRollbackFor = VenueBookingLeadTimePolicy.MinimumLeadTimeNotMetException.class)
    public BookingDto accept(UUID inquiryId) {
        User scout = requireRole(UserRole.SCOUT);
        if(policyAcceptances!=null) policyAcceptances.requireCurrentPolicies(scout);
        AvailabilityInquiry inquiry = locked(inquiryId);
        if (!inquiry.getScoutId().equals(scout.getId())) throw new ForbiddenException("Only the inquiry scout can accept this offer");
        requireStatus(inquiry, AvailabilityInquiryStatus.OFFERED);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!inquiry.getOfferExpiresAt().isAfter(now)) {
            throw new ConflictException("This availability offer has expired");
        }
        // Serialize new commitments with moderation and normal booking acceptance.
        Venue venue = venues.findByIdForUpdate(inquiry.getVenueId())
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        if (venue.getStatus() != VenueStatus.ACTIVE) throw new NotFoundException("Venue is not active");
        // Lock contention must not extend an offer beyond its deadline.
        now = OffsetDateTime.now(clock);
        if (!inquiry.getOfferExpiresAt().isAfter(now)) {
            throw new ConflictException("This availability offer has expired");
        }
        if (!leadTimePolicy.isSatisfied(venue, inquiry.getOfferedDate(), inquiry.getOfferedStartTime())) {
            expire(inquiry, now);
            throw leadTimePolicy.notMet(venue);
        }
        ensureNoBookingConflict(inquiry.getVenueId(), inquiry.getOfferedDate(), inquiry.getOfferedStartTime(), inquiry.getOfferedEndTime());
        BookingDto booking = bookingService.createApprovedExceptionBooking(
                inquiry.getVenueId(), scout.getId(), inquiry.getOfferedDate(),
                inquiry.getOfferedStartTime(), inquiry.getOfferedEndTime(), inquiry.getMessage());
        inquiry.setStatus(AvailabilityInquiryStatus.ACCEPTED);
        inquiry.setAcceptedAt(now);
        inquiry.setAcceptedBookingId(booking.id());
        return booking;
    }

    @Transactional
    public int expireOffers(OffsetDateTime now) {
        List<AvailabilityInquiry> expired = inquiries.lockDueOffers(now, 100);
        expired.forEach(value -> expire(value, now));
        return expired.size();
    }

    private void expire(AvailabilityInquiry inquiry, OffsetDateTime now) {
        inquiry.setStatus(AvailabilityInquiryStatus.EXPIRED);
        inquiry.setExpiredAt(now);
        eventPublisher.publishEvent(new AvailabilityInquiryLifecycleEvent(
                inquiry.getId(), AvailabilityInquiryLifecycleEvent.Type.OFFER_EXPIRED));
    }

    private void ensureNoBookingConflict(UUID venueId, LocalDate date, LocalTime start, LocalTime end) {
        boolean conflict = bookings.findBlockingBookings(venueId, date).stream()
                .anyMatch(booking -> BookingOverlapRule.overlaps(start, end, booking.getStartTime(), booking.getEndTime()));
        if (conflict) throw new ConflictException("The offered interval conflicts with an existing booking");
    }

    private void validateFutureInterval(Venue venue, LocalDate date, LocalTime start, LocalTime end) {
        BookingOverlapRule.requireValid(start, end);
        ZoneId zone;
        try { zone = ZoneId.of(venue.getTimezone()); }
        catch (DateTimeException exception) { throw new IllegalStateException("Venue timezone is invalid", exception); }
        if (date == null || start == null) throw new BadRequestException("requested start is required");
    }

    private User requireRole(UserRole role) {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != role) throw new ForbiddenException("This operation requires the " + role + " role");
        return user;
    }

    private Venue activeVenue(UUID id) {
        Venue venue = venues.findById(id).orElseThrow(() -> new NotFoundException("Venue not found"));
        if (venue.getStatus() != VenueStatus.ACTIVE) throw new NotFoundException("Venue is not active");
        return venue;
    }

    private AvailabilityInquiry locked(UUID id) {
        return inquiries.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Availability inquiry not found"));
    }

    private Venue ownedVenue(AvailabilityInquiry inquiry, User owner) {
        return ownedVenue(inquiry, owner, false);
    }

    private Venue ownedVenue(AvailabilityInquiry inquiry, User owner, boolean lock) {
        Venue venue = (lock ? venues.findByIdForUpdate(inquiry.getVenueId()) : venues.findById(inquiry.getVenueId())).orElseThrow(() -> new NotFoundException("Venue not found"));
        if (venue.getOwner() == null || !owner.getId().equals(venue.getOwner().getId()))
            throw new ForbiddenException("Only the venue owner can respond to this inquiry");
        return venue;
    }

    private void requireStatus(AvailabilityInquiry inquiry, AvailabilityInquiryStatus expected) {
        if (inquiry.getStatus() != expected) throw new ConflictException("Inquiry is not " + expected.name().toLowerCase());
    }

    private AvailabilityInquiryDto dto(AvailabilityInquiry inquiry) {
        Venue venue = venues.findById(inquiry.getVenueId()).orElseThrow(() -> new NotFoundException("Venue not found"));
        return dto(inquiry, venue);
    }

    private AvailabilityInquiryDto dto(AvailabilityInquiry inquiry, Venue venue) {
        return new AvailabilityInquiryDto(inquiry.getId(), inquiry.getVenueId(), venue.getName(), venue.getTimezone(), inquiry.getScoutId(),
                inquiry.getRequestedDate(), inquiry.getRequestedStartTime(), inquiry.getRequestedEndTime(), inquiry.getMessage(), inquiry.getStatus(),
                inquiry.getOfferedDate(), inquiry.getOfferedStartTime(), inquiry.getOfferedEndTime(), inquiry.getOfferExpiresAt(), inquiry.getAcceptedBookingId(),
                inquiry.getCreatedAt(), inquiry.getDeclinedAt(), inquiry.getOfferedAt(), inquiry.getAcceptedAt(), inquiry.getExpiredAt());
    }
}
