package com.eema.backend.service;

import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.request.ScoutContactDto;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingEvent;
import com.eema.backend.entity.BookingEventPayload;
import com.eema.backend.entity.enums.BookingEventType;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import com.eema.backend.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing bookings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final BookingEventRepository bookingEventRepository;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    /**
     * Create a new booking.
     *
     * @param request booking request
     * @param scoutId ID of the scout making the booking
     * @return created booking DTO
     * @throws NotFoundException if the venue is not found or not active
     */
    public BookingDto createBooking(CreateBookingRequest request, UUID scoutId) {
        // Load venue and check if active
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new NotFoundException("Venue not found with id: " + request.venueId()));
        if (!venue.getStatus().equals(VenueStatus.ACTIVE)) {
            throw new NotFoundException("Venue is not active");
        }

        // Load scout user
        User scout = userRepository.findById(scoutId)
                .orElseThrow(() -> new NotFoundException("Scout not found with id: " + scoutId));

        // Build scout contact JSON (from request + user data)
        ScoutContactDto scoutContactDto = request.scoutContact();
        var scoutContactMap = new java.util.HashMap<String, Object>();
        scoutContactMap.put("name", scoutContactDto.name());
        scoutContactMap.put("email", scoutContactDto.email());
        scoutContactMap.put("phone", scoutContactDto.phone());
        scoutContactMap.put("scoutId", scoutId.toString());
        String scoutContactJson;
        try {
            scoutContactJson = objectMapper.writeValueAsString(scoutContactMap);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing scout contact", e);
        }

        // Load owner user (from venue)
        User owner = venue.getOwner();
        if (owner == null) {
            throw new NotFoundException("Owner not found for venue");
        }

        // Build owner contact JSON (from user data)
        var ownerContactMap = new java.util.HashMap<String, Object>();
        ownerContactMap.put("name", owner.getFullName());
        ownerContactMap.put("email", owner.getEmail());
        ownerContactMap.put("phone", owner.getPhone());
        ownerContactMap.put("ownerId", owner.getId().toString());
        String ownerContactJson;
        try {
            ownerContactJson = objectMapper.writeValueAsString(ownerContactMap);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing owner contact", e);
        }

        // Compute financial fields
        BigDecimal venuePriceEur = venue.getDayRateEur(); // price the scout pays for the venue
        // TODO: Determine if totalPaidEur should include mollieFeeEur (scout bears Mollie fee) or not.
        BigDecimal totalPaidEur = venuePriceEur; // amount paid by scout (same as venue price initially)
        // TODO: Commission percentage should be configurable (e.g., via app.properties)
        BigDecimal commissionPercentage = new BigDecimal("5.00"); // placeholder commission percentage
        BigDecimal commissionEur = venuePriceEur.multiply(commissionPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP); // platform commission
        // TODO: Mollie fee will be set by webhook; determine who bears the fee (scout or owner) for payout calculation.
        BigDecimal mollieFeeEur = BigDecimal.ZERO; // placeholder, will be updated later by Mollie webhook
        // TODO: Determine if ownerPayoutEur should subtract mollieFeeEur (if owner bears fee) or not (if scout bears fee).
        BigDecimal ownerPayoutEur = venuePriceEur.subtract(commissionEur).subtract(mollieFeeEur); // amount to owner after platform commission and mollie fee

        // Create and save booking
        Booking booking = Booking.builder()
                .venueId(venue.getId())
                .scoutId(scoutId)
                .molliePaymentId(null) // Will be set later by payment service
                .status(BookingStatus.PENDING)
                .venuePriceEur(venuePriceEur)
                .totalPaidEur(totalPaidEur)
                .commissionPercentage(commissionPercentage)
                .commissionEur(commissionEur)
                .mollieFeeEur(mollieFeeEur)
                .ownerPayoutEur(ownerPayoutEur)
                .shootDate(request.shootDate())
                .startTime(LocalTime.of(9, 0)) // default start time
                .endTime(LocalTime.of(17, 0)) // default end time (ensure start < end)
                .rawBrief(request.rawBrief())
                .scoutContact(scoutContactJson)
                .ownerContact(ownerContactJson)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Create initial event: BOOKING_CREATED
        appendEvent(savedBooking.getId(), BookingEventType.BOOKING_CREATED, scoutId.toString(), Map.of());

        return toBookingDto(savedBooking);
    }

    /**
     * Get bookings for a user based on their role.
     *
     * @param userId user ID
     * @param role   role of the user (SCOUT, OWNER, ADMIN)
     * @return list of booking DTOs
     */
    public List<BookingDto> getBookingsForUser(UUID userId, UserRole role) {
        List<Booking> bookings;
        switch (role) {
            case SCOUT:
                bookings = bookingRepository.findByScoutIdOrderByCreatedAtDesc(userId);
                break;
            case OWNER:
                // Get all venue IDs owned by the user, then find bookings for those venues
                var venueIds = venueRepository.findByOwnerId(userId)
                        .stream()
                        .map(Venue::getId)
                        .collect(Collectors.toList());
                if (venueIds.isEmpty()) {
                    bookings = List.of();
                } else {
                    bookings = bookingRepository.findByVenueIdIn(venueIds);
                }
                break;
            case ADMIN:
                bookings = bookingRepository.findAll();
                break;
            default:
                bookings = List.of();
        }
        return bookings.stream()
                .map(this::toBookingDto)
                .collect(Collectors.toList());
    }

    /**
     * Update the booking status and append an event.
     * Internal method used by webhook service.
     *
     * @param bookingId booking ID
     * @param newStatus new status to set
     * @param createdBy who is making the change (user ID as string) – ignored; authenticated user is used
     * @param payload   additional data for the event
     */
    public void updateBookingStatus(UUID bookingId, BookingStatus newStatus, String createdBy, Map<String, Object> payload) {
        // Obtain authentication and ensure user is authenticated and not anonymous
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ForbiddenException("Authentication required");
        }
        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        // Authorization checks
        boolean allowed = false;
        if (user.getRole() == UserRole.ADMIN) {
            allowed = true;
        } else if (user.getRole() == UserRole.SCOUT) {
            // Scouts can only update their own bookings
            if (booking.getScoutId().equals(user.getId())) {
                allowed = true;
            }
        } else if (user.getRole() == UserRole.OWNER) {
            // Venue owners can only update bookings for their venues
            Venue venue = venueRepository.findById(booking.getVenueId())
                    .orElseThrow(() -> new NotFoundException("Venue not found for booking: " + bookingId));
            if (venue.getOwner().getId().equals(user.getId())) {
                allowed = true;
            }
        }
        // Other roles (if any) not allowed by default

        if (!allowed) {
            throw new ForbiddenException("User not authorized to update booking status");
        }

        // Use authenticated user for event data
        String userIdStr = user.getId().toString();

        booking.setStatus(newStatus);
        // Set the appropriate timestamp based on the status
        switch (newStatus) {
            case PAID -> booking.setPaidAt(OffsetDateTime.now());
            case CONFIRMED -> booking.setConfirmedAt(OffsetDateTime.now());
            case CANCELLED -> booking.setCancelledAt(OffsetDateTime.now());
            default -> {
                // No timestamp for other statuses
            }
        }
        bookingRepository.save(booking);

        // Append event using authenticated user
        appendEvent(bookingId, getEventTypeForStatus(newStatus, bookingId, userIdStr), userIdStr, payload);
    }

    /**
     * Append an event to a booking.
     * Internal method.
     *
     * @param bookingId booking ID
     * @param type      event type
     * @param createdBy who created the event (as string representation of user ID)
     * @param payload   event payload (will be serialized to JSON)
     */
    public void appendEvent(UUID bookingId, BookingEventType type, String createdBy, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing event payload", e);
        }

        // Convert createdBy string to UUID for actorId, use the string as actorName (temporary)
        UUID actorId = UUID.fromString(createdBy);
        BookingEvent event = BookingEvent.builder()
                .bookingId(bookingId)
                .eventType(type)
                .payload(BookingEventPayload.builder().data((Map<String, Object>) payload).build())
                .actorId(actorId)
                .actorName(createdBy)
                .build();

        bookingEventRepository.save(event);
    }

    /**
     * Determine the event type based on the booking status and who triggered the change.
     * This is used when updating the status to generate an appropriate event.
     *
     * @param status the booking status
     * @param bookingId the booking ID
     * @param createdBy the user ID (as string) who triggered the change
     * @return the corresponding event type
     */
    private BookingEventType getEventTypeForStatus(BookingStatus status, UUID bookingId, String createdBy) {
        return switch (status) {
            case PAID -> BookingEventType.PAYMENT_SUCCEEDED;
            case CONFIRMED -> BookingEventType.OWNER_CONFIRMED;
            case CANCELLED -> {
                // Determine if the cancellation was initiated by the scout or owner
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
                Venue venue = venueRepository.findById(booking.getVenueId())
                        .orElseThrow(() -> new NotFoundException("Venue not found for booking: " + bookingId));
                UUID userId = UUID.fromString(createdBy);

                if (userId.equals(booking.getScoutId())) {
                    yield BookingEventType.SCOUT_CANCELLED;
                } else if (userId.equals(venue.getOwner().getId())) {
                    yield BookingEventType.OWNER_CANCELLED;
                } else {
                    // Neither scout nor owner triggered the change - default to SCOUT_CANCELLED
                    // This could happen for system-triggered cancellations
                    yield BookingEventType.SCOUT_CANCELLED;
                }
            }
            default -> BookingEventType.BOOKING_CREATED; // fallback, though not expected
        };
    }

    /**
     * Convert Booking entity to BookingDto.
     */
    private BookingDto toBookingDto(Booking booking) {
        return new BookingDto(
                booking.getId(),
                booking.getVenueId(),
                booking.getScoutId(),
                booking.getStatus(),
                booking.getShootDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getRawBrief(),
                booking.getVenuePriceEur(),
                booking.getCommissionPercentage(),
                booking.getCommissionEur(),
                booking.getMollieFeeEur(),
                booking.getOwnerPayoutEur(),
                booking.getTotalPaidEur(),
                booking.getMolliePaymentId(),
                booking.getScoutContact(),
                booking.getOwnerContact(),
                booking.getCreatedAt(),
                booking.getPaidAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt()
        );
    }
}