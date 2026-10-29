package com.eema.backend.integration;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import com.eema.backend.entity.enums.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Approved v1 paid-booking boundary; the canonical calculator matrix is unchanged. */
class ZeroPriceIntegrationTest extends InquirySupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 void pricing(UUID v,String rate){jdbc.update("update venues set hourly_rate_eur=?,minimum_booking_minutes=1,full_day_rate_eur=null where id=?",new BigDecimal(rate),v);}
 void minimumError(com.fasterxml.jackson.databind.JsonNode error){assertEquals(400,error.path("status").asInt());assertTrue(error.path("message").asText().contains("minimum bookable amount"));assertTrue(error.path("message").asText().contains("0.01"));}
 @Test void roundedZeroRequestHasNoPersistenceReservationOrLifecycleEffects()throws Exception{
  UUID v=inquiryVenue();pricing(v,"0.01");var calendarBefore=calendar(v);var availabilityBefore=available(v);long notices=events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count();int committed=hooks.committedBookings.size();
  minimumError(req(post("/api/bookings"),scout,body(v,"09:00","09:01"),400));
  assertEquals(0,rows("bookings","venue_id",v));assertEquals(calendarBefore,calendar(v));assertEquals(availabilityBefore,available(v));assertEquals(notices,events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count());assertEquals(committed,hooks.committedBookings.size());verifyNoInteractions(mollie);
  // The rejected interval remains bookable once the owner supplies a valid price.
  pricing(v,"0.30");UUID b=create(v,scout,"09:00","09:01");assertEquals(new BigDecimal("0.01"),jdbc.queryForObject("select venue_price_eur from bookings where id=?",BigDecimal.class,b));
 }
 @ParameterizedTest @ValueSource(strings={"0.30","0.60"})
 void oneCentFinalAmountIsAcceptedWithoutCheckout(String rate)throws Exception{
  UUID v=inquiryVenue();pricing(v,rate);UUID b=create(v,scout,"09:00","09:01");assertEquals(new BigDecimal("0.01"),jdbc.queryForObject("select venue_price_eur from bookings where id=?",BigDecimal.class,b));assertEquals("REQUESTED",state(b));assertEquals("NOT_STARTED",jdbc.queryForObject("select payment_creation_status from bookings where id=?",String.class,b));verifyNoInteractions(mollie);
 }
 @Test void inquiryAcceptanceUsesCurrentPriceAndRollsBackAtomically()throws Exception{
  UUID v=inquiryVenue();pricing(v,"0.30");UUID i=open(v,scout);offer(i,owner,date,"18:00","18:01",200);pricing(v,"0.01");String before=inquiryRow(i);var calendarBefore=calendar(v);long inquiryNotices=noticesFor(i);int bookingNotices=hooks.committedBookings.size();
  minimumError(inquiryAction(i,"accept",scout,400));assertEquals(before,inquiryRow(i));assertEquals("OFFERED",inquiryState(i));assertEquals(0,rows("bookings","venue_id",v));assertEquals(inquiryNotices,noticesFor(i));assertEquals(bookingNotices,hooks.committedBookings.size());assertEquals(calendarBefore,calendar(v));verifyNoInteractions(mollie);
  pricing(v,"0.30");UUID b=uuid(inquiryAction(i,"accept",scout,200));assertEquals("AWAITING_PAYMENT",state(b));assertEquals(new BigDecimal("0.01"),jdbc.queryForObject("select venue_price_eur from bookings where id=?",BigDecimal.class,b));
 }
 @ParameterizedTest @EnumSource(PaymentCreationStatus.class)
 void legacyZeroCheckoutFailsWithoutChangingClaimOrCallingProvider(PaymentCreationStatus status)throws Exception{
  UUID v=inquiryVenue(),b=create(v,scout,"09:00","10:00");action(b,"accept",owner,200);
  // Isolated disposable database: simulate a pre-V13 legacy row, then restore the constraint.
  jdbc.execute("alter table bookings drop constraint if exists bookings_minimum_venue_price");
  try{
   jdbc.update("update bookings set venue_price_eur=0,payment_creation_status=?,payment_idempotency_key=?,payment_creation_started_at=now() where id=?",status.name(),status==PaymentCreationStatus.NOT_STARTED?null:UUID.randomUUID(),b);
   String before=row(b);long notices=notices(b);minimumError(req(post("/api/bookings/"+b+"/checkout"),scout,null,400));assertEquals(before,row(b));assertEquals(notices,notices(b));verifyNoInteractions(mollie);
  }finally{jdbc.update("update bookings set venue_price_eur=125.50 where id=?",b);jdbc.execute("alter table bookings add constraint bookings_minimum_venue_price check (venue_price_eur >= 0.01)");}
 }
 @Test void databaseRejectsZeroOriginalPriceAcrossAllBookingStates()throws Exception{
  UUID v=inquiryVenue(),b=create(v,scout,"09:00","10:00");for(var status:BookingStatus.values())assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set status=?::booking_status,venue_price_eur=0 where id=?",status.name(),b));
  assertEquals("REQUESTED",state(b));assertTrue(jdbc.queryForObject("select convalidated from pg_constraint where conname='bookings_minimum_venue_price'",Boolean.class));assertEquals(1,jdbc.queryForObject("select count(*) from flyway_schema_history where version='13' and success",Integer.class));
 }
}
