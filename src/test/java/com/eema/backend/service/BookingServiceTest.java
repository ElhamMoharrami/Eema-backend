package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.UserRole;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookingRepository;
    @Mock VenueRepository venueRepository;
    @Mock UserRepository userRepository;
    @Mock BookingEventRepository bookingEventRepository;
    AppProperties appProperties = new AppProperties(null, null, null, null, null, null,
            new AppProperties.CommissionProperties(new BigDecimal("20.00")), null);
    @Mock CurrentUserService currentUserService;
    @Spy BookingStateMachine stateMachine = new BookingStateMachine();
    @Spy BookingFinancials financials = new BookingFinancials();
    BookingService service;

    private UUID bookingId;
    private UUID scoutId;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepository, venueRepository, userRepository, bookingEventRepository,
                appProperties, currentUserService, stateMachine, financials);
        bookingId = UUID.randomUUID();
        scoutId = UUID.randomUUID();
        booking = Booking.builder()
                .id(bookingId).scoutId(scoutId).venueId(UUID.randomUUID())
                .status(BookingStatus.PENDING).venuePriceEur(new BigDecimal("100.00"))
                .commissionPercentage(new BigDecimal("20.00")).commissionEur(new BigDecimal("20.00"))
                .mollieFeeEur(new BigDecimal("0.00")).ownerPayoutEur(new BigDecimal("80.00"))
                .totalPaidEur(new BigDecimal("0.00")).molliePaymentId("tr_expected").build();
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
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void duplicatePaidWebhookDoesNotCreateAnotherEvent() {
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        service.processPaidPayment(paidPayment("tr_expected", "100.00"));
        verifyNoInteractions(bookingEventRepository);
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
    void ownerCanConfirmPaidBookingButScoutCannotConfirm() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("owner@example.com").role(UserRole.OWNER).build();
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(venueRepository.findById(booking.getVenueId())).thenReturn(Optional.of(
                Venue.builder().id(booking.getVenueId()).owner(owner).name("Venue").build()));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        assertEquals(BookingStatus.CONFIRMED, service.confirmBooking(bookingId).status());

        booking.setStatus(BookingStatus.PAID);
        when(currentUserService.getCurrentUser()).thenReturn(User.builder().id(scoutId).role(UserRole.SCOUT).build());
        assertThrows(ForbiddenException.class, () -> service.confirmBooking(bookingId));
    }

    @Test
    void pendingCancelSucceedsAndPaidCancelIsRejected() {
        User scout = User.builder().id(scoutId).email("scout@example.com").role(UserRole.SCOUT).build();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(currentUserService.getCurrentUser()).thenReturn(scout);
        assertEquals(BookingStatus.CANCELLED, service.cancelBooking(bookingId).status());

        booking.setStatus(BookingStatus.PAID);
        assertThrows(ConflictException.class, () -> service.cancelBooking(bookingId));
    }

    private MolliePaymentResponse paidPayment(String id, String amount) {
        return new MolliePaymentResponse(id, "paid",
                new MolliePaymentResponse.Amount(amount, "EUR"), null,
                Map.of("bookingId", bookingId.toString()));
    }
}
