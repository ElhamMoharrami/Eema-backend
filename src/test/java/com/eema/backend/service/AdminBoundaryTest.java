package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBoundaryTest {
 @Test void nestedValuesCannotBypassEventRedaction(){var b=mock(BookingRepository.class);var e=mock(BookingEventRepository.class);UUID id=UUID.randomUUID();when(b.findById(id)).thenReturn(Optional.of(Booking.builder().id(id).build()));when(e.findPageByBookingIdOrderByCreatedAtAsc(eq(id),any())).thenReturn(new PageImpl<>(List.of(BookingEvent.builder().payload(new BookingEventPayload(Map.of("amount",100,"reason",Map.of("token","QA-secret"),"source",List.of(Map.of("password","QA-secret"))))).build())));var s=new AdminOperationsService(b,e,mock(OwnerPayoutRepository.class),mock(OwnerFinancialLiabilityRepository.class),mock(OwnerPayoutService.class),mock(OwnerFinancialLiabilityService.class),new OperationalThresholds(60,1440,1440,72,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC)),mock(OperationalExceptionRepository.class));var result=s.events(id,PageRequest.of(0,20)).getContent().getFirst().payload();assertEquals(Map.of("amount",100),result);}
}
