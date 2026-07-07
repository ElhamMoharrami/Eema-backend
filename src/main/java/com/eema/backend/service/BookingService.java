package com.eema.backend.service;

import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.request.ScoutContactDto;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingEvent;
import com.eema.backend.entity.BookingEventType;
import com.eema.backend.entity.BookingStatus;
import com.eema.backend.entity.User;
import com.eema.backend.entity.UserRole;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.VenueStatus;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

        // Calculate amounts
        BigDecimal bookingAmount = venue.getDayRateEur();
        BigDecimal serviceFee = bookingAmount.multiply(new BigDecimal("0.04"))
                .max(new BigDecimal("5.00"))
                .min(new BigDecimal("20.00"));
        BigDecimal eeemaCommission = serviceFee;
        BigDecimal ownerPayout = bookingAmount.subtract(eeemaCommission);

        // Load scout user
        User scout = userRepository.findById(scoutId)
                .orElseThrow(() -> new NotFoundException("Scout not found with id: " + scoutId));

        // Build scout contact JSON (from request + user data)
        ScoutContactDto scoutContactDto = request.scoutContact();
        // We can also add the scout's email from the user entity if needed, but the request already has email.
        // However, we might want to override with the user's email? The requirement says:
        // "Load scout user. Build scoutContact JSON from the request + user data."
        // We'll use the request data, but we can also include the user's ID or name if needed.
        // For simplicity, we'll use the request data as is, but we can also add the user's ID.
        // Let's create a map that includes the request fields and the scout's user ID.
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

        // Create and save booking
        Booking booking = Booking.builder()
                .venueId(venue.getId())
                .scoutId(scoutId)
                .molliePaymentId(null) // Will be set later by payment service
                .status(BookingStatus.PENDING)
                .bookingAmountEur(bookingAmount)
                .serviceFeeEur(serviceFee)
                .eeemaCommissionEur(eeemaCommission)
                .ownerPayoutEur(ownerPayout)
                .shootDate(request.shootDate())
                .rawBrief(request.rawBrief())
                .scoutContact(scoutContactJson)
                .ownerContact(ownerContactJson)
                .calaVerified(false)
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
                bookings = bookingRepository.findByScoutId(userId);
                break;
            case OWNER:
                // Get all venue IDs owned by the user, then find bookings for those venues
                List<UUID> venueIds = venueRepository.findByOwnerId(userId)
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
     * @param createdBy who is making the change (user ID)
     * @param payload   additional data for the event
     */
    public void updateBookingStatus(UUID bookingId, BookingStatus newStatus, String createdBy, Map<String, Object> payload) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

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

        // Append event
        appendEvent(bookingId, getEventTypeForStatus(newStatus), createdBy, payload);
    }

    /**
     * Append an event to a booking.
     * Internal method.
     *
     * @param bookingId booking ID
     * @param type      event type
     * @param createdBy who created the event
     * @param payload   event payload (will be serialized to JSON)
     */
    public void appendEvent(UUID bookingId, BookingEventType type, String createdBy, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing event payload", e);
        }

        BookingEvent event = BookingEvent.builder()
                .bookingId(bookingId)
                .eventType(type)
                .payload(payloadJson)
                .createdBy(createdBy)
                .build();

        bookingEventRepository.save(event);
    }

    /**
     * Determine the event type based on the booking status.
     * This is used when updating the status to generate an appropriate event.
     *
     * @param status the booking status
     * @return the corresponding event type
     */
    private BookingEventType getEventTypeForStatus(BookingStatus status) {
        return switch (status) {
            case PAID -> BookingEventType.PAYMENT_SUCCEEDED;
            case CONFIRMED -> BookingEventType.BOOKING_CONFIRMED;
            case CANCELLED -> BookingEventType.CANCELLATION_REQUESTED;
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
                booking.getMolliePaymentId(),
                booking.getStatus(),
                booking.getBookingAmountEur(),
                booking.getServiceFeeEur(),
                booking.getEeemaCommissionEur(),
                booking.getOwnerPayoutEur(),
                booking.getShootDate(),
                booking.getRawBrief(),
                booking.getScoutContact(),
                booking.getOwnerContact(),
                booking.isCalaVerified(),
                booking.getCreatedAt(),
                booking.getPaidAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt()
        );
    }
}