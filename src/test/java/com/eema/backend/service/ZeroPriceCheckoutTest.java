package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZeroPriceCheckoutTest {
 @ParameterizedTest @ValueSource(strings={"0.00","-0.01","0.009"})
 void invalidStoredPriceCannotReachProviderOrCreateClaim(String value){
  var repository=mock(BookingRepository.class);var provider=mock(MollieProviderClient.class);
  var now=Instant.parse("2026-09-19T12:00:00Z");var coordinator=new PaymentCreationCoordinator(repository,new BookingFinancials(),Clock.fixed(now,ZoneOffset.UTC),null);
  var service=new PaymentService(coordinator,provider);UUID id=UUID.randomUUID(),scout=UUID.randomUUID();
  var b=Booking.builder().id(id).scoutId(scout).status(BookingStatus.AWAITING_PAYMENT).venuePriceEur(new BigDecimal(value)).paymentExpiresAt(OffsetDateTime.ofInstant(now.plusSeconds(3600),ZoneOffset.UTC)).build();
  when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(b));
  var error=assertThrows(BadRequestException.class,()->service.createPayment(id,scout));assertTrue(error.getMessage().contains("minimum bookable amount"));assertEquals(PaymentCreationStatus.NOT_STARTED,b.getPaymentCreationStatus());assertNull(b.getPaymentIdempotencyKey());assertNull(b.getPaymentCreationStartedAt());verifyNoInteractions(provider);
 }
}
