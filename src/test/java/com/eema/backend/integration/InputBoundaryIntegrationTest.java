package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** INPUT structural boundaries through HTTP and disposable PostgreSQL, including rollback evidence. */
class InputBoundaryIntegrationTest extends InquirySupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @AfterEach void noProvider(){verifyNoInteractions(mollie);}
 @Test void nullWeeklyElementIs400AndLeavesCalendarUntouched()throws Exception{
  UUID v=venue(owner,60);var before=jdbc.queryForList("select * from venue_operating_hours where venue_id=? order by id",v);
  req(put("/api/venues/"+v+"/availability/weekly"),owner,Map.of("intervals",Arrays.asList((Object)null)),400);
  assertEquals(before,jdbc.queryForList("select * from venue_operating_hours where venue_id=? order by id",v));
 }
 @Test void scoutPhoneBoundaryRejectsBeforePersistenceAndNotifications()throws Exception{
  UUID v=venue(owner,60);var body=body(v,"09:00","10:00");body.put("scoutContact",Map.of("name","QA Scout","email","contact@example.test","phone","1".repeat(51)));
  long notices=jdbc.queryForObject("select count(*) from notification_outbox",Long.class);
  req(post("/api/bookings"),scout,body,400);assertEquals(0,rows("bookings","venue_id",v));assertEquals(notices,jdbc.queryForObject("select count(*) from notification_outbox",Long.class));
  body.put("scoutContact",Map.of("name","QA Scout","email","contact@example.test","phone","1".repeat(50)));UUID b=uuid(req(post("/api/bookings"),scout,body,201));assertEquals("1".repeat(50),jdbc.queryForObject("select scout_contact_phone from bookings where id=?",String.class,b));
 }
 @Test void scoutEmailCannotOverflowSnapshotColumn()throws Exception{
  String email="a".repeat(64)+"@"+"b".repeat(63)+"."+"c".repeat(63)+"."+"d".repeat(60)+".nl";assertEquals(256,email.length());
  UUID v=venue(owner,60);var body=body(v,"09:00","10:00");body.put("scoutContact",Map.of("name","QA Scout","email",email));long notices=jdbc.queryForObject("select count(*) from notification_outbox",Long.class);
  req(post("/api/bookings"),scout,body,400);assertEquals(0,rows("bookings","venue_id",v));assertEquals(notices,jdbc.queryForObject("select count(*) from notification_outbox",Long.class));
 }
 @Test void validRegisteredOwnerName120SurvivesBookingSnapshot()throws Exception{
  String name="N".repeat(120);var actor=call("register",Map.of("email",address(),"fullName",name,"password",QA_PASSWORD,"role","OWNER"),201);UUID v=venue(actor,60);
  UUID b=create(v,scout,"09:00","10:00");assertEquals(name,jdbc.queryForObject("select owner_contact_name from bookings where id=?",String.class,b));assertEquals(name,detail(b).path("ownerContact").path("name").asText());
 }

 @Test void validRegisteredScoutName120SurvivesInquiryAcceptance()throws Exception{
  String name="S".repeat(120);var actor=call("register",Map.of("email",address(),"fullName",name,"password",QA_PASSWORD,"role","SCOUT"),201);
  call("verify-email",Map.of("token",verifier(id(actor))),200);acceptPolicies(actor);UUID v=inquiryVenue(),i=open(v,actor);offer(i,owner,date,"18:00","20:00",200);UUID b=uuid(inquiryAction(i,"accept",actor,200));
  assertEquals(name,jdbc.queryForObject("select scout_contact_name from bookings where id=?",String.class,b));assertEquals("ACCEPTED",inquiryState(i));
 }
 @Test void historicalOwnerWithoutNameKeepsSupportedEmailFallback()throws Exception{
  String email="a".repeat(64)+"@"+"b".repeat(63)+"."+"c".repeat(63)+"."+"d".repeat(58)+".nl";assertEquals(254,email.length());
  var actor=call("register",Map.of("email",email,"fullName","QA Owner","password",QA_PASSWORD,"role","OWNER"),201);jdbc.update("update users set full_name=null where id=?",id(actor));
  UUID b=create(venue(actor,60),scout,"09:00","10:00");assertEquals(email,jdbc.queryForObject("select owner_contact_name from bookings where id=?",String.class,b));
 }
}
