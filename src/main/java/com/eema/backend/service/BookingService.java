package com.eema.backend.service;

import com.eema.backend.config.AppProperties;

import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.request.ScoutContactRequest;
import com.eema.backend.dto.response.BookingDto;
import com.eema.backend.dto.response.MolliePaymentResponse;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingContactSnapshot;
import com.eema.backend.entity.BookingEvent;
import com.eema.backend.entity.BookingEventPayload;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;

import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.BookingEventType;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.entity.enums.VenueStatus;

import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.NotFoundException;

import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final BookingEventRepository bookingEventRepository;

    private final AppProperties appProperties;
    private final CurrentUserService currentUserService;
    private final BookingStateMachine stateMachine;
    private final BookingFinancials financials;

    // --------------------------------------------------
    // CREATE BOOKING
    // --------------------------------------------------

    @Transactional
    public BookingDto createBooking(CreateBookingRequest request, UUID scoutId) {

        Venue venue = venueRepository.findById(request.venueId()).orElseThrow(() -> new NotFoundException("Venue not found with id: " + request.venueId()));

        if (venue.getStatus() != VenueStatus.ACTIVE) {
            throw new NotFoundException("Venue is not active");
        }

        User scout = userRepository.findById(scoutId).orElseThrow(() -> new NotFoundException("Scout not found with id: " + scoutId));

        if (scout.getRole() != UserRole.SCOUT) {
            throw new ForbiddenException("Only SCOUT users can create bookings");
        }

        User owner = venue.getOwner();

        if (owner == null) {
            throw new NotFoundException("Venue owner not found");
        }

        BigDecimal venuePriceEur = money(venue.getDayRateEur());

        BigDecimal commissionPercentage = validateCommissionPercentage(appProperties.commission().percentage());

        BigDecimal commissionEur = calculateCommission(venuePriceEur, commissionPercentage);

        /*
         * Mollie's actual fee is not known yet.
         * It will be reconciled later.
         */
        BigDecimal mollieFeeEur = money(BigDecimal.ZERO);

        /*
         * Scout pays exactly the venue price.
         */
        BigDecimal totalPaidEur = money(BigDecimal.ZERO);

        /*
         * Provisional payout until the real Mollie fee is known.
         */
        BigDecimal ownerPayoutEur = calculateOwnerPayout(venuePriceEur, commissionEur, mollieFeeEur);

        Booking booking = Booking.builder().venueId(venue.getId()).scoutId(scout.getId()).molliePaymentId(null).status(BookingStatus.PENDING)

                .venuePriceEur(venuePriceEur).totalPaidEur(totalPaidEur).commissionPercentage(commissionPercentage).commissionEur(commissionEur).mollieFeeEur(mollieFeeEur).ownerPayoutEur(ownerPayoutEur)

                .shootDate(request.shootDate()).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0)).rawBrief(request.rawBrief())

                /*
                 * These are now typed embedded objects,
                 * not JSON strings.
                 */.scoutContact(buildScoutContact(request.scoutContact())).ownerContact(buildOwnerContact(owner))

                .build();

        Booking savedBooking = bookingRepository.save(booking);

        appendEvent(savedBooking.getId(), BookingEventType.BOOKING_CREATED, scout.getId(), actorName(scout), ActorType.SCOUT, Map.of());

        log.info("Booking created: bookingId={}, venueId={}, scoutId={}, venuePrice={}, commission={}, provisionalOwnerPayout={}", savedBooking.getId(), venue.getId(), scout.getId(), venuePriceEur, commissionEur, ownerPayoutEur);

        return toBookingDto(savedBooking);
    }

    // --------------------------------------------------
    // GET BOOKINGS
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<BookingDto> getBookingsForUser(UUID userId, UserRole role) {

        List<Booking> bookings = switch (role) {

            case SCOUT -> bookingRepository.findByScoutIdOrderByCreatedAtDesc(userId);

            case OWNER -> getOwnerBookings(userId);

            case ADMIN -> bookingRepository.findAll();
        };

        return bookings.stream().map(this::toBookingDto).toList();
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingForCurrentUser(UUID bookingId) {
        User user = currentUserService.getCurrentUser();
        Booking booking = getBooking(bookingId);
        authorizeAccess(user, booking);
        return toBookingDto(booking);
    }

    @Transactional(readOnly = true)
    public Optional<String> findRefundReference(String paymentId) {
        return bookingRepository.findByMolliePaymentId(paymentId).map(Booking::getMollieRefundId);
    }

    @Transactional
    public BookingDto confirmBooking(UUID bookingId) {
        User user = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));
        authorizeOwnerOrAdmin(user, booking);
        transition(booking, BookingStatus.CONFIRMED);
        appendEvent(bookingId, BookingEventType.OWNER_CONFIRMED, user.getId(), actorName(user), actorTypeForUser(user), Map.of());
        return toBookingDto(booking);
    }

    @Transactional
    public BookingDto cancelBooking(UUID bookingId) {
        User user = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));
        authorizeAccess(user, booking);
        transition(booking, BookingStatus.CANCELLED);
        BookingEventType eventType = switch (user.getRole()) {
            case SCOUT -> BookingEventType.SCOUT_CANCELLED;
            case OWNER -> BookingEventType.OWNER_CANCELLED;
            case ADMIN -> BookingEventType.ADMIN_CANCELLED;
        };
        appendEvent(bookingId, eventType, user.getId(), actorName(user), actorTypeForUser(user), Map.of());
        return toBookingDto(booking);
    }

    private List<Booking> getOwnerBookings(UUID ownerId) {

        List<UUID> venueIds = venueRepository.findByOwnerId(ownerId).stream().map(Venue::getId).toList();

        if (venueIds.isEmpty()) {
            return List.of();
        }

        return bookingRepository.findByVenueIdIn(venueIds);
    }

    // --------------------------------------------------
    // USER STATUS CHANGES
    // --------------------------------------------------

    @Transactional
    public void updateBookingStatusByUser(UUID bookingId, BookingStatus newStatus, Map<String, Object> payload) {

        User user = currentUserService.getCurrentUser();

        Booking booking = getBooking(bookingId);

        authorizeUserUpdate(user, booking);

        BookingEventType eventType = determineUserEventType(newStatus, user);

        transition(booking, newStatus);

        bookingRepository.save(booking);

        appendEvent(booking.getId(), eventType, user.getId(), actorName(user), actorTypeForUser(user), safePayload(payload));

        log.info("Booking status updated by user: bookingId={}, status={}, userId={}, role={}", bookingId, newStatus, user.getId(), user.getRole());
    }

    // --------------------------------------------------
    // MOLLIE PAYMENT
    // --------------------------------------------------

    @Transactional
    public void processPaidPayment(MolliePaymentResponse payment) {

        validateMolliePayment(payment);

        UUID bookingId = extractBookingId(payment);

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        /*
         * Protect against applying another Mollie payment
         * to this booking.
         */
        if (booking.getMolliePaymentId() == null || !booking.getMolliePaymentId().equals(payment.id())) {

            throw new IllegalStateException("Mollie payment ID does not match booking");
        }

        /*
         * Mollie may deliver the same webhook multiple times.
         */
        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.REFUNDED) {

            log.info("Booking {} already PAID - duplicate Mollie webhook ignored", bookingId);

            return;
        }

        BigDecimal paidAmount = extractPaidAmount(payment);

        BigDecimal expectedAmount = money(booking.getVenuePriceEur());

        /*
         * Scout pays exactly the venue price.
         */
        if (paidAmount.compareTo(expectedAmount) != 0) {

            throw new IllegalStateException("Paid amount does not match venue price");
        }

        booking.setMolliePaymentId(payment.id());

        booking.setTotalPaidEur(paidAmount);

        /*
         * Still zero until actual Mollie fee is reconciled.
         */
        BigDecimal mollieFeeEur = money(BigDecimal.ZERO);

        booking.setMollieFeeEur(mollieFeeEur);
        booking.setMollieFeeReconciled(false);

        BigDecimal commissionEur = calculateCommission(booking.getVenuePriceEur(), booking.getCommissionPercentage());

        booking.setCommissionEur(commissionEur);

        BigDecimal ownerPayoutEur = calculateOwnerPayout(booking.getVenuePriceEur(), commissionEur, mollieFeeEur);

        booking.setOwnerPayoutEur(ownerPayoutEur);

        transition(booking, BookingStatus.PAID);

        bookingRepository.save(booking);

        Map<String, Object> payload = new HashMap<>();

        payload.put("provider", "Mollie");

        payload.put("paymentId", payment.id());

        payload.put("amount", payment.amount().value());

        payload.put("currency", payment.amount().currency());

        appendEvent(booking.getId(), BookingEventType.PAYMENT_SUCCEEDED, null, "Mollie", ActorType.WEBHOOK, payload);

        log.info("Booking marked PAID: bookingId={}, paymentId={}, commission={}, provisionalOwnerPayout={}", booking.getId(), payment.id(), commissionEur, ownerPayoutEur);
    }

    // --------------------------------------------------
    // MOLLIE FEE RECONCILIATION
    // --------------------------------------------------

    @Transactional
    public void reconcileMollieFee(UUID bookingId, BigDecimal mollieFeeEur) {

        if (mollieFeeEur == null || mollieFeeEur.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException("Mollie fee must be non-negative");
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.CONFIRMED) {

            throw new IllegalStateException("Cannot reconcile Mollie fee for unpaid booking");
        }

        BigDecimal normalizedFee = money(mollieFeeEur);

        if (booking.isMollieFeeReconciled()) {
            if (money(booking.getMollieFeeEur()).compareTo(normalizedFee) == 0) {
                return;
            }
            throw new ConflictException("Mollie fee has already been reconciled");
        }

        BigDecimal ownerPayout = calculateOwnerPayout(booking.getVenuePriceEur(), booking.getCommissionEur(), normalizedFee);

        booking.setMollieFeeEur(normalizedFee);
        booking.setMollieFeeReconciled(true);
        booking.setMollieFeeReconciledAt(OffsetDateTime.now());

        booking.setOwnerPayoutEur(ownerPayout);

        bookingRepository.save(booking);

        appendEvent(bookingId, BookingEventType.PAYMENT_RECONCILED, null, "SYSTEM", ActorType.SYSTEM, Map.of("mollieFeeEur", normalizedFee, "commissionEur", booking.getCommissionEur(), "ownerPayoutEur", ownerPayout));

        log.info("Mollie fee reconciled: bookingId={}, mollieFee={}, commission={}, ownerPayout={}", bookingId, normalizedFee, booking.getCommissionEur(), ownerPayout);
    }

    // --------------------------------------------------
    // INTERNAL SYSTEM STATUS CHANGE
    // --------------------------------------------------

    @Transactional
    public void updateBookingStatusFromSystem(UUID bookingId, BookingStatus newStatus, BookingEventType eventType, Map<String, Object> payload) {

        Booking booking = getBooking(bookingId);

        transition(booking, newStatus);

        bookingRepository.save(booking);

        appendEvent(bookingId, eventType, null, "SYSTEM", ActorType.SYSTEM, safePayload(payload));

        log.info("Booking status updated by system: bookingId={}, status={}", bookingId, newStatus);
    }

    // --------------------------------------------------
    // AUTHORIZATION
    // --------------------------------------------------

    private void authorizeUserUpdate(User user, Booking booking) {
        authorizeAccess(user, booking);
    }

    private void authorizeAccess(User user, Booking booking) {

        if (user.getRole() == UserRole.ADMIN) {

            return;
        }

        if (user.getRole() == UserRole.SCOUT) {

            if (booking.getScoutId().equals(user.getId())) {

                return;
            }

            throw new ForbiddenException("Scout cannot modify another scout's booking");
        }

        if (user.getRole() == UserRole.OWNER) {

            Venue venue = venueRepository.findById(booking.getVenueId()).orElseThrow(() -> new NotFoundException("Venue not found for booking"));

            if (venue.getOwner() != null && venue.getOwner().getId().equals(user.getId())) {

                return;
            }
        }

        throw new ForbiddenException("User is not authorized to modify this booking");
    }

    private void authorizeOwnerOrAdmin(User user, Booking booking) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (user.getRole() != UserRole.OWNER) {
            throw new ForbiddenException("Only the venue owner or admin can confirm a booking");
        }
        Venue venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new NotFoundException("Venue not found for booking"));
        if (venue.getOwner() == null || !venue.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Owner cannot modify a booking for another owner's venue");
        }
    }

    // --------------------------------------------------
    // STATUS RULES
    // --------------------------------------------------

    private void transition(Booking booking, BookingStatus newStatus) {
        stateMachine.requireTransition(booking.getStatus(), newStatus);
        booking.setStatus(newStatus);

        OffsetDateTime now = OffsetDateTime.now();

        switch (newStatus) {

            case PAID -> booking.setPaidAt(now);

            case CONFIRMED -> booking.setConfirmedAt(now);

            case CANCELLED -> booking.setCancelledAt(now);

            default -> {
                // No timestamp required.
            }
        }
    }

    private BookingEventType determineUserEventType(BookingStatus status, User user) {

        return switch (status) {

            case CONFIRMED -> {

                if (user.getRole() != UserRole.OWNER && user.getRole() != UserRole.ADMIN) {

                    throw new ForbiddenException("Only the venue owner or admin can confirm a booking");
                }

                yield BookingEventType.OWNER_CONFIRMED;
            }

            case CANCELLED -> {

                if (user.getRole() == UserRole.SCOUT) {

                    yield BookingEventType.SCOUT_CANCELLED;
                }

                if (user.getRole() == UserRole.OWNER) {

                    yield BookingEventType.OWNER_CANCELLED;
                }

                /*
                 * If you add ADMIN_CANCELLED,
                 * use it here instead.
                 */
                yield BookingEventType.SCOUT_CANCELLED;
            }

            case PAID -> throw new ForbiddenException("PAID status can only be set through Mollie");

            default -> throw new IllegalArgumentException("Unsupported user-driven booking status: " + status);
        };
    }

    // --------------------------------------------------
    // MOLLIE HELPERS
    // --------------------------------------------------

    private void validateMolliePayment(MolliePaymentResponse payment) {

        if (payment == null) {
            throw new IllegalArgumentException("Mollie payment must not be null");
        }

        if (payment.id() == null || payment.id().isBlank()) {

            throw new IllegalArgumentException("Mollie payment ID is missing");
        }

        if (!"paid".equalsIgnoreCase(payment.status())) {

            throw new IllegalArgumentException("Mollie payment is not paid");
        }

        if (payment.metadata() == null) {
            throw new IllegalStateException("Mollie payment metadata is missing");
        }

        if (payment.amount() == null || payment.amount().value() == null || payment.amount().currency() == null) {

            throw new IllegalStateException("Mollie payment amount is missing");
        }

        if (!"EUR".equalsIgnoreCase(payment.amount().currency())) {

            throw new IllegalStateException("Unexpected payment currency: " + payment.amount().currency());
        }
    }

    private UUID extractBookingId(MolliePaymentResponse payment) {

        String bookingIdValue = payment.metadata().get("bookingId");

        if (bookingIdValue == null || bookingIdValue.isBlank()) {

            throw new IllegalStateException("Mollie payment has no bookingId metadata");
        }

        try {

            return UUID.fromString(bookingIdValue);

        } catch (IllegalArgumentException e) {

            throw new IllegalStateException("Invalid bookingId in Mollie metadata", e);
        }
    }

    private BigDecimal extractPaidAmount(MolliePaymentResponse payment) {

        try {

            return money(new BigDecimal(payment.amount().value()));

        } catch (NumberFormatException e) {

            throw new IllegalStateException("Invalid Mollie payment amount", e);
        }
    }

    // --------------------------------------------------
    // FINANCIAL HELPERS
    // --------------------------------------------------

    private BigDecimal calculateCommission(BigDecimal venuePrice, BigDecimal commissionPercentage) {
        return financials.commission(venuePrice, commissionPercentage);
    }

    private BigDecimal validateCommissionPercentage(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalStateException("Commission percentage must be between 0 and 100");
        }
        return percentage.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOwnerPayout(BigDecimal venuePrice, BigDecimal commission, BigDecimal mollieFee) {
        return financials.ownerPayout(venuePrice, commission, mollieFee);
    }

    private BigDecimal money(BigDecimal value) {
        return financials.money(value);
    }

    // --------------------------------------------------
    // EVENT CREATION
    // --------------------------------------------------

    private void appendEvent(UUID bookingId, BookingEventType eventType, UUID actorId, String actorName, ActorType actorType, Map<String, Object> payload) {

        BookingEventPayload eventPayload = BookingEventPayload.builder().data(safePayload(payload)).build();

        BookingEvent event = BookingEvent.builder().bookingId(bookingId).eventType(eventType).payload(eventPayload).actorId(actorId).actorName(actorName != null ? actorName : actorType.name()).actorType(actorType).build();

        bookingEventRepository.save(event);
    }

    private ActorType actorTypeForUser(User user) {

        return switch (user.getRole()) {

            case SCOUT -> ActorType.SCOUT;

            case OWNER -> ActorType.OWNER;

            case ADMIN -> ActorType.ADMIN;
        };
    }

    private String actorName(User user) {

        if (user.getFullName() != null && !user.getFullName().isBlank()) {

            return user.getFullName();
        }

        return user.getEmail();
    }

    // --------------------------------------------------
    // CONTACT SNAPSHOTS
    // --------------------------------------------------

    private BookingContactSnapshot buildScoutContact(ScoutContactRequest contact) {

        return BookingContactSnapshot.builder().name(contact.name()).email(contact.email()).phone(contact.phone()).build();
    }

    private BookingContactSnapshot buildOwnerContact(User owner) {

        String ownerName = owner.getFullName();

        if (ownerName == null || ownerName.isBlank()) {

            ownerName = owner.getEmail();
        }

        return BookingContactSnapshot.builder().name(ownerName).email(owner.getEmail()).phone(owner.getPhone()).build();
    }

    // --------------------------------------------------
    // GENERAL HELPERS
    // --------------------------------------------------

    private Booking getBooking(UUID bookingId) {

        return bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));
    }

    private Map<String, Object> safePayload(Map<String, Object> payload) {

        return payload != null ? payload : Map.of();
    }

    // --------------------------------------------------
    // DTO
    // --------------------------------------------------

    private BookingDto toBookingDto(Booking booking) {

        return new BookingDto(booking.getId(), booking.getVenueId(), booking.getScoutId(), booking.getStatus(),

                booking.getShootDate(), booking.getStartTime(), booking.getEndTime(),

                booking.getRawBrief(),

                booking.getVenuePriceEur(), booking.getCommissionPercentage(), booking.getCommissionEur(), booking.getMollieFeeEur(), booking.isMollieFeeReconciled(), booking.getMollieFeeReconciledAt(), booking.getOwnerPayoutEur(), booking.getOwnerPayoutStatus(), booking.getTotalPaidEur(),

                booking.getMolliePaymentId(), booking.getPaymentCreationStatus(), booking.getRefundStatus(), booking.getMollieRefundId(), booking.getRefundAmountEur(), booking.getRefundRequestedAt(), booking.getRefundedAt(),

                booking.getScoutContact(), booking.getOwnerContact(),

                booking.getCreatedAt(), booking.getPaidAt(), booking.getConfirmedAt(), booking.getCancelledAt());
    }
}
