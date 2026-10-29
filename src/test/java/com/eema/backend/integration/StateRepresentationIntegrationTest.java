package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Mapping-only historical fixtures, never a new event producer or domain transition. */
class StateRepresentationIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Autowired BookingEventRepository bookingEvents;
 @Autowired MarketplaceTrustEventRepository trustEvents;
 @Test void everyBookingAuditEventAndActorSurvivesPostgresMapping(){
  UUID v=venue(owner,60),b=UUID.randomUUID();jdbc.update("""
  insert into bookings(id,venue_id,scout_id,status,shoot_date,start_time,end_time,venue_price_eur,hourly_rate_eur_snapshot,minimum_booking_minutes_snapshot,duration_minutes,billable_minutes,commission_percentage,commission_eur,total_paid_eur,scout_contact_name,scout_contact_email,owner_contact_name,owner_contact_email,owner_response_expires_at)
  values(?,?,?,'REQUESTED',current_date+14,'09:00','10:00',100,100,60,60,60,20,20,0,'QA Scout','qa@example.test','QA Owner','owner@example.test',now()+interval '1 day')
  """,b,v,id(scout));String before=row(b);int n=0;
  for(BookingEventType type:BookingEventType.values()){
   ActorType actor=ActorType.values()[n++%ActorType.values().length];var stored=bookingEvents.saveAndFlush(BookingEvent.builder().bookingId(b).eventType(type).actorType(actor).actorId(switch(actor){case SCOUT->id(scout);case OWNER->id(owner);case ADMIN->id(admin);default->null;}).actorName("QA mapping fixture").payload(new BookingEventPayload(Map.of("source","QA fixture"))).build());
   var loaded=bookingEvents.findById(stored.getId()).orElseThrow();assertEquals(type,loaded.getEventType());assertEquals(actor,loaded.getActorType());assertEquals(type.name(),jdbc.queryForObject("select event_type::text from booking_events where id=?",String.class,stored.getId()));assertEquals(actor.name(),jdbc.queryForObject("select actor_type from booking_events where id=?",String.class,stored.getId()));
  }
  assertEquals(Arrays.stream(BookingEventType.values()).map(Enum::name).toList(),jdbc.queryForList("select enumlabel from pg_enum e join pg_type t on t.oid=e.enumtypid where t.typname='booking_event_type' order by enumsortorder",String.class));
  assertEquals(before,row(b));verifyNoInteractions(mollie);
 }
 @Test void everyTrustEventAndSubjectSurvivesPostgresMapping(){
  int n=0;for(TrustEventType type:TrustEventType.values()){
   TrustSubjectType subject=TrustSubjectType.values()[n%TrustSubjectType.values().length];ActorType actor=ActorType.values()[n++%ActorType.values().length];UUID subjectId=UUID.randomUUID();
   var stored=trustEvents.saveAndFlush(MarketplaceTrustEvent.builder().subjectType(subject).subjectId(subjectId).eventType(type).actorType(actor).note("QA historical representation fixture").build());var loaded=trustEvents.findById(stored.getId()).orElseThrow();assertEquals(subject,loaded.getSubjectType());assertEquals(type,loaded.getEventType());assertEquals(actor,loaded.getActorType());assertEquals(subjectId,loaded.getSubjectId());
   assertEquals(type.name(),jdbc.queryForObject("select event_type from marketplace_trust_events where id=?",String.class,stored.getId()));
  }
  verifyNoInteractions(mollie);
 }
}
