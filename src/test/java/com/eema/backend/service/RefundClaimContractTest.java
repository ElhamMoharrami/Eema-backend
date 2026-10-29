package com.eema.backend.service;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundClaimContractTest {
    final BookingRepository bookings=mock(BookingRepository.class);
    final BookingEventRepository events=mock(BookingEventRepository.class);
    final OwnerPayoutService payouts=mock(OwnerPayoutService.class);
    final Instant instant=Instant.parse("2026-06-01T12:00:00Z");
    Booking booking;
    @BeforeEach void setup(){booking=Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED)
            .paidAt(OffsetDateTime.ofInstant(instant.minusSeconds(60),ZoneOffset.UTC)).molliePaymentId("tr_paid")
            .totalPaidEur(new BigDecimal("1000.00")).build();when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));}
    RefundCoordinator coordinator(Instant now){return new RefundCoordinator(bookings,events,new BookingFinancials(),new BookingStateMachine(),mock(ApplicationEventPublisher.class),payouts,Clock.fixed(now,ZoneOffset.UTC));}
    RefundCoordinator.Claim claim(Instant now){return coordinator(now).claim(booking.getId(),UUID.randomUUID(),"Admin");}
    @ParameterizedTest @ValueSource(ints={3599,3600,3601}) void exactSafeReplayWindow(int seconds){var first=claim(instant);if(seconds<3600)assertEquals(first,claim(instant.plusSeconds(seconds)));else assertThrows(ConflictException.class,()->claim(instant.plusSeconds(seconds)));assertEquals(first.idempotencyKey(),booking.getRefundIdempotencyKey());}
    @ParameterizedTest @EnumSource(value=RefundStatus.class,names={"FAILED","CANCELED"}) void terminalRetryCannotInventNewIdentity(RefundStatus state){var first=claim(instant);booking.setRefundStatus(state);booking.setTotalPaidEur(new BigDecimal("2000.00"));assertThrows(ConflictException.class,()->claim(instant));assertEquals(first.idempotencyKey(),booking.getRefundIdempotencyKey());assertEquals(first.amount(),booking.getRefundAmountEur());}
    @ParameterizedTest @EnumSource(value=RefundStatus.class,names={"FAILED","CANCELED"})
    void lateSafetyClaimCannotReplaceTerminalIdentity(RefundStatus state) {
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setLatePaymentDetectedAt(OffsetDateTime.ofInstant(instant,ZoneOffset.UTC));
        var first=coordinator(instant).claimLatePayment(booking.getId()).orElseThrow();
        booking.setRefundStatus(state);
        assertThrows(ConflictException.class,()->coordinator(instant).claimLatePayment(booking.getId()));
        assertEquals(first.idempotencyKey(),booking.getRefundIdempotencyKey());
        assertEquals(first.amount(),booking.getRefundAmountEur());
    }
    @ParameterizedTest @ValueSource(strings={"0.00","-1.00"}) void invalidPaidAmountCreatesNothing(String amount){booking.setTotalPaidEur(new BigDecimal(amount));assertThrows(ConflictException.class,()->claim(instant));assertNull(booking.getRefundIdempotencyKey());verifyNoInteractions(events,payouts);}
    @Test void notSuccessfullyPaidCreatesNothing(){booking.setPaidAt(null);assertThrows(ConflictException.class,()->claim(instant));assertNull(booking.getRefundIdempotencyKey());}
    @Test void existingAmountNeverRecomputedOnRetry(){var first=claim(instant);booking.setTotalPaidEur(new BigDecimal("2000.00"));assertEquals(first,claim(instant.plusSeconds(30)));}
    @ParameterizedTest @ValueSource(ints={409,429,500,502}) void uncertainHttpResponseMustKeepClaim(int status){var current=mock(CurrentUserService.class);var provider=mock(MollieProviderClient.class);var coordinator=mock(RefundCoordinator.class);var admin=User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).email("qa@example.test").build();when(current.getCurrentUser()).thenReturn(admin);when(coordinator.claim(any(),any(),anyString())).thenReturn(new RefundCoordinator.Claim(booking.getId(),"tr_paid",new BigDecimal("1000.00"),UUID.randomUUID()));when(provider.createRefund(anyString(),any(),any(),any())).thenThrow(status>=500?org.springframework.web.client.HttpServerErrorException.create(org.springframework.http.HttpStatus.valueOf(status),"",org.springframework.http.HttpHeaders.EMPTY,new byte[0],null):org.springframework.web.client.HttpClientErrorException.create(org.springframework.http.HttpStatus.valueOf(status),"",org.springframework.http.HttpHeaders.EMPTY,new byte[0],null));assertThrows(BadRequestException.class,()->new RefundService(current,coordinator,provider,mock(BookingService.class)).requestFullRefund(booking.getId()));verify(coordinator,never()).markRejected(any());}
}
