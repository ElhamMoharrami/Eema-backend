package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.config.BookingWorkflowProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.request.ScoutContactRequest;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationEventPublisher;
import com.eema.backend.event.BookingPaidEvent;
import com.eema.backend.event.BookingLifecycleEvent;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookingRepository;
    @Mock VenueRepository venueRepository;
    @Mock UserRepository userRepository;
    @Mock BookingEventRepository bookingEventRepository;
    AppProperties appProperties = new AppProperties(null, null, null, null, null, null,
            new AppProperties.CommissionProperties(new BigDecimal("20.00")), null, null);
    @Mock CurrentUserService currentUserService;
    @Spy BookingStateMachine stateMachine = new BookingStateMachine();
    @Spy BookingFinancials financials = new BookingFinancials();
    @Spy BookingPriceCalculator priceCalculator = new BookingPriceCalculator();
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock BookingAvailabilityValidator availabilityValidator;
    @Mock OwnerPayoutService ownerPayoutService;
    @Mock VenueBookingLeadTimePolicy leadTimePolicy;
    BookingService service;

    private UUID bookingId;
    private UUID scoutId;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepository, venueRepository, userRepository, bookingEventRepository,
                appProperties, currentUserService, stateMachine, financials, priceCalculator, eventPublisher, availabilityValidator,
                new BookingWorkflowProperties(24, 24, 60000),
                new BookingCancellationPolicy(financials), ownerPayoutService, leadTimePolicy);
        org.mockito.Mockito.lenient().when(leadTimePolicy.isSatisfied(any(), any(), any())).thenReturn(true);
        bookingId = UUID.randomUUID();
        scoutId = UUID.randomUUID();
        booking = Booking.builder()
                .id(bookingId).scoutId(scoutId).venueId(UUID.randomUUID())
                .status(BookingStatus.AWAITING_PAYMENT).paymentExpiresAt(OffsetDateTime.now().plusHours(1))
                .shootDate(LocalDate.now().plusDays(10)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(14, 0))
                .venuePriceEur(new BigDecimal("100.00"))
                .commissionPercentage(new BigDecimal("20.00")).commissionEur(new BigDecimal("20.00"))
                .mollieFeeEur(new BigDecimal("0.00")).ownerPayoutEur(new BigDecimal("80.00"))
                .totalPaidEur(new BigDecimal("0.00")).molliePaymentId("tr_expected").build();
        org.mockito.Mockito.lenient().when(venueRepository.findById(booking.getVenueId())).thenReturn(Optional.of(
                Venue.builder().id(booking.getVenueId()).timezone("Europe/Amsterdam").build()));
    }

    @Test
    void scoutCanReadOwnBookingButNotAnotherScoutsBooking() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        User scout = User.builder().id(scoutId).role(UserRole.SCOUT).build();
        when(currentUserService.getCurrentUser()).thenReturn(scout);
        assertEquals(bookingId, service.getBookingForCurrentUser(bookingId).id());

        when(currentUserService.getCurrentUser())
                .thenReturn(User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build());
        assertThrows(ForbiddenException.class, () -> service.getBookingForCurrentUser(bookingId));
    }

    @Test
    void rejectsWebhookWhosePaymentIdDoesNotMatchStoredPayment() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        MolliePaymentResponse payment = paidPayment("tr_wrong", "100.00");
        assertThrows(IllegalStateException.class, () -> service.processPaidPayment(payment));
    }

    @Test
    void rejectsWebhookWhoseAmountDoesNotMatchDatabasePrice() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        MolliePaymentResponse payment = paidPayment("tr_expected", "99.99");
        assertThrows(IllegalStateException.class, () -> service.processPaidPayment(payment));
        assertEquals(BookingStatus.AWAITING_PAYMENT, booking.getStatus());
    }

    @Test
    void duplicatePaidWebhookDoesNotCreateAnotherEvent() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        verifyNoInteractions(bookingEventRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void firstPaidTransitionPublishesNotificationOnceAndDuplicateDoesNot() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(eventPublisher, times(1)).publishEvent(new BookingPaidEvent(bookingId));
    }

    @Test
    void ownerAndAdminCanReadAuthorizedBooking() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).role(UserRole.OWNER).build();
        Venue venue = Venue.builder().id(booking.getVenueId()).owner(owner).name("Venue").build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(venueRepository.findById(booking.getVenueId())).thenReturn(Optional.of(venue));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        assertEquals(bookingId, service.getBookingForCurrentUser(bookingId).id());

        when(currentUserService.getCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).build());
        assertEquals(bookingId, service.getBookingForCurrentUser(bookingId).id());
    }

    @Test
    void databaseConflictFromConcurrentAttemptCannotCreateSecondBooking() {
        User owner = User.builder().id(UUID.randomUUID()).email("owner@example.com").fullName("Owner").role(UserRole.OWNER).build();
        User scout = User.builder().id(scoutId).email("scout@example.com").role(UserRole.SCOUT).build();
        Venue venue = Venue.builder().id(booking.getVenueId()).owner(owner).name("Venue").status(VenueStatus.ACTIVE)
                .timezone("Europe/Amsterdam")
                .hourlyRateEur(new BigDecimal("25.00")).minimumBookingMinutes(240)
                .fullDayRateEur(new BigDecimal("100.00")).build();
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(userRepository.findById(scoutId)).thenReturn(Optional.of(scout));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("exclusion constraint"));
        CreateBookingRequest request = new CreateBookingRequest(venue.getId(), LocalDate.now().plusDays(1),
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(17, 0), "Brief",
                new ScoutContactRequest("Scout", "scout@example.com", null));

        assertThrows(ConflictException.class, () -> service.createBooking(request, scoutId));
        verifyNoInteractions(bookingEventRepository);
    }

    @Test
    void requestAcceptPaymentEndsConfirmedWithoutSecondOwnerAction() {
        User owner = ownerForBooking();
        prepareRequestedBooking(owner);

        assertEquals(BookingStatus.AWAITING_PAYMENT, service.acceptRequest(bookingId).status());
        verifyNoInteractions(priceCalculator);
        service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        verify(eventPublisher, times(1)).publishEvent(new BookingPaidEvent(bookingId));
    }

    @Test
    void scoutCreatesRequestWithSelectedTimesWithoutStartingPayment() {
        User owner = ownerForBooking();
        User scout = User.builder().id(scoutId).email("scout@example.com").role(UserRole.SCOUT).build();
        Venue venue = Venue.builder().id(booking.getVenueId()).owner(owner).name("Venue").status(VenueStatus.ACTIVE)
                .timezone("Europe/Amsterdam")
                .hourlyRateEur(new BigDecimal("175.00")).minimumBookingMinutes(240)
                .fullDayRateEur(new BigDecimal("1200.00")).build();
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(userRepository.findById(scoutId)).thenReturn(Optional.of(scout));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(bookingId);
            return saved;
        });
        CreateBookingRequest request = new CreateBookingRequest(venue.getId(), LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(14, 0), "Brief",
                new ScoutContactRequest("Scout", "scout@example.com", null));

        var result = service.createBooking(request, scoutId);
        assertEquals(BookingStatus.REQUESTED, result.status());
        assertEquals(LocalTime.of(10, 0), result.startTime());
        assertEquals(LocalTime.of(14, 0), result.endTime());
        assertEquals(new BigDecimal("700.00"), result.venuePriceEur());
        assertEquals(new BigDecimal("175.00"), result.hourlyRateEurSnapshot());
        assertEquals(240, result.minimumBookingMinutesSnapshot());
        assertEquals(240, result.durationMinutes());
        assertEquals(240, result.billableMinutes());
        assertEquals(null, result.molliePaymentId());
        org.junit.jupiter.api.Assertions.assertNotNull(result.ownerResponseExpiresAt());
    }

    @Test
    void ownerCanDeclineRequestAndDoubleAcceptanceIsRejected() {
        User owner = ownerForBooking();
        prepareRequestedBooking(owner);
        assertEquals(BookingStatus.DECLINED, service.declineRequest(bookingId).status());

        booking.setStatus(BookingStatus.REQUESTED);
        service.acceptRequest(bookingId);
        assertThrows(ConflictException.class, () -> service.acceptRequest(bookingId));
        verify(eventPublisher, times(1)).publishEvent(new BookingLifecycleEvent(
                bookingId, BookingLifecycleEvent.Type.REQUEST_ACCEPTED));
    }

    @Test
    void unauthorizedOwnerCannotAcceptRequest() {
        User actualOwner = ownerForBooking();
        prepareRequestedBooking(actualOwner);
        when(currentUserService.getCurrentUser()).thenReturn(
                User.builder().id(UUID.randomUUID()).email("other@example.com").role(UserRole.OWNER).build());
        assertThrows(ForbiddenException.class, () -> service.acceptRequest(bookingId));
    }

    @Test
    void acceptedRequestMakesOnlyOverlappingRequestsUnavailable() {
        User owner = ownerForBooking();
        prepareRequestedBooking(owner);
        Booking overlapping = Booking.builder().id(UUID.randomUUID()).venueId(booking.getVenueId())
                .status(BookingStatus.REQUESTED).shootDate(booking.getShootDate())
                .startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(18, 0)).build();
        when(bookingRepository.findOverlappingRequestedForUpdate(booking.getVenueId(), booking.getShootDate(),
                booking.getStartTime(), booking.getEndTime(), bookingId)).thenReturn(java.util.List.of(overlapping));

        service.acceptRequest(bookingId);

        assertEquals(BookingStatus.AWAITING_PAYMENT, booking.getStatus());
        assertEquals(BookingStatus.UNAVAILABLE, overlapping.getStatus());
        verify(eventPublisher).publishEvent(new BookingLifecycleEvent(
                overlapping.getId(), BookingLifecycleEvent.Type.REQUEST_BECAME_UNAVAILABLE));
    }

    @Test
    void acceptanceAfterOwnerDeadlineExpiresRequestAndFailsClearly() {
        User owner = ownerForBooking();
        prepareRequestedBooking(owner);
        booking.setOwnerResponseExpiresAt(OffsetDateTime.now().minusMinutes(1));

        assertThrows(com.eema.backend.exception.RequestDeadlineExpiredException.class,
                () -> service.acceptRequest(bookingId));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        verify(availabilityValidator, never()).validate(any(), any(), any(), any(), any());
    }

    @Test
    void acceptanceThatHasBecomeTooLateExpiresRequest() {
        User owner = ownerForBooking();
        prepareRequestedBooking(owner);
        Venue venue = venueRepository.findByIdForUpdate(booking.getVenueId()).orElseThrow();
        VenueBookingLeadTimePolicy.MinimumLeadTimeNotMetException failure =
                new VenueBookingLeadTimePolicy.MinimumLeadTimeNotMetException("Minimum lead time not met");
        when(leadTimePolicy.isSatisfied(venue, booking.getShootDate(), booking.getStartTime())).thenReturn(false);
        when(leadTimePolicy.notMet(venue)).thenReturn(failure);

        assertEquals(failure, assertThrows(VenueBookingLeadTimePolicy.MinimumLeadTimeNotMetException.class,
                () -> service.acceptRequest(bookingId)));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        verify(availabilityValidator, never()).validate(any(), any(), any(), any(), any());
    }

    @Test
    void dueRequestExpirationIsIdempotentAndIgnoresAcceptedRows() {
        OffsetDateTime now = OffsetDateTime.now();
        Booking due = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.REQUESTED)
                .ownerResponseExpiresAt(now.minusSeconds(1)).build();
        when(bookingRepository.lockDueRequestedBookings(now, 100)).thenReturn(java.util.List.of(due));
        assertEquals(1, service.expireDueRequests(now, 100));
        assertEquals(BookingStatus.EXPIRED, due.getStatus());
        when(bookingRepository.lockDueRequestedBookings(now, 100)).thenReturn(java.util.List.of());
        assertEquals(0, service.expireDueRequests(now, 100));
    }

    @Test
    void approvedRequestExpiresAndLatePaymentIsMarkedForRefund() {
        User owner = ownerForBooking();
        prepareRequestedBooking(owner);
        service.acceptRequest(bookingId);
        OffsetDateTime afterDeadline = booking.getPaymentExpiresAt().plusSeconds(1);
        service.expireApproval(bookingId, afterDeadline);
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        var outcome = service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        assertEquals(BookingService.PaidPaymentResult.LATE_PAYMENT, outcome.result());
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(new BigDecimal("100.00"), booking.getTotalPaidEur());
        verify(eventPublisher, never()).publishEvent(any(BookingPaidEvent.class));
    }

    @Test
    void paymentAfterDeadlineExpiresBookingEvenBeforeSchedulerRuns() {
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setPaymentExpiresAt(OffsetDateTime.now().minusSeconds(1));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        var outcome = service.processPaidPayment(paidPayment("tr_expected", "100.00"));

        assertEquals(BookingService.PaidPaymentResult.LATE_PAYMENT, outcome.result());
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        verify(eventPublisher, never()).publishEvent(any(BookingPaidEvent.class));
    }

    @Test
    void acceptedInquiryBookingPublishesOnlyAcceptedBookingLifecycleEmail() {
        User owner = ownerForBooking();
        User scout = User.builder().id(scoutId).email("scout@example.com").fullName("Scout")
                .role(UserRole.SCOUT).build();
        Venue venue = Venue.builder().id(booking.getVenueId()).owner(owner).name("Studio")
                .timezone("Europe/Amsterdam").status(VenueStatus.ACTIVE)
                .hourlyRateEur(new BigDecimal("25.00"))
                .minimumBookingMinutes(240).fullDayRateEur(new BigDecimal("100.00")).build();
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(userRepository.findById(scoutId)).thenReturn(Optional.of(scout));
        when(bookingRepository.findBlockingBookings(venue.getId(), LocalDate.now().plusDays(3))).thenReturn(java.util.List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(bookingId);
            return saved;
        });

        var result = service.createApprovedExceptionBooking(venue.getId(), scoutId,
                LocalDate.now().plusDays(3), LocalTime.of(7, 0), LocalTime.of(9, 0), "Exception time");

        assertEquals(BookingStatus.AWAITING_PAYMENT, result.status());
        verify(eventPublisher, times(1)).publishEvent(new BookingLifecycleEvent(
                bookingId, BookingLifecycleEvent.Type.REQUEST_ACCEPTED));
    }

    private User ownerForBooking() {
        return User.builder().id(UUID.randomUUID()).email("owner@example.com").fullName("Owner").role(UserRole.OWNER).build();
    }

    private void prepareRequestedBooking(User owner) {
        booking.setStatus(BookingStatus.REQUESTED);
        booking.setShootDate(LocalDate.now().plusDays(2));
        booking.setStartTime(LocalTime.of(9, 0));
        booking.setEndTime(LocalTime.of(17, 0));
        booking.setOwnerResponseExpiresAt(OffsetDateTime.now().plusHours(24));
        booking.setMolliePaymentId("tr_expected");
        booking.setScoutContact(com.eema.backend.entity.BookingContactSnapshot.builder().name("Scout").email("scout@example.com").build());
        booking.setOwnerContact(com.eema.backend.entity.BookingContactSnapshot.builder().name("Owner").email("owner@example.com").build());
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        Venue venue = Venue.builder().id(booking.getVenueId()).owner(owner).status(VenueStatus.ACTIVE).timezone("Europe/Amsterdam").build();
        when(venueRepository.findById(booking.getVenueId())).thenReturn(Optional.of(venue));
        when(venueRepository.findByIdForUpdate(booking.getVenueId())).thenReturn(Optional.of(venue));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
    }

    private MolliePaymentResponse paidPayment(String id, String amount) {
        return new MolliePaymentResponse(id, "paid",
                new MolliePaymentResponse.Amount(amount, "EUR"), null,
                Map.of("bookingId", bookingId.toString()));
    }
}
