package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="eema.admin.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18089")
class AdminBrowserIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Test void adminReadAndRecoveryJourneys()throws Exception{
  List<UUID> ids=new ArrayList<>();for(int n=0;n<25;n++)ids.add(create(venue(owner,60),scout,"09:00","10:00"));UUID b=ids.get(0);
  for(int n=0;n<24;n++)jdbc.update("insert into booking_events(id,booking_id,event_type,actor_name,actor_type,payload,created_at) values (?,?,'PAYMENT_SUCCEEDED','QA SYSTEM','SYSTEM',?::jsonb,now()+(? * interval '1 second'))",UUID.randomUUID(),b,"{\"data\":{\"amountEur\":125.50,\"reason\":{\"token\":\"QA-nested-secret\"},\"password\":\"QA-top-secret\"}}",n+1);
  jdbc.update("update bookings set payment_creation_status='CREATING',payment_creation_started_at=now()-interval '2 hours' where id=?",b);
  for(int n=25;n<201;n++)jdbc.update("""
   insert into bookings(id,venue_id,scout_id,status,shoot_date,start_time,end_time,venue_price_eur,hourly_rate_eur_snapshot,minimum_booking_minutes_snapshot,duration_minutes,billable_minutes,commission_percentage,commission_eur,total_paid_eur,scout_contact_name,scout_contact_email,owner_contact_name,owner_contact_email,owner_response_expires_at)
   select ?,venue_id,scout_id,'REQUESTED',shoot_date,start_time,end_time,venue_price_eur,hourly_rate_eur_snapshot,minimum_booking_minutes_snapshot,duration_minutes,billable_minutes,commission_percentage,commission_eur,0,scout_contact_name,scout_contact_email,owner_contact_name,owner_contact_email,owner_response_expires_at from bookings where id=?
   """,UUID.randomUUID(),b);
  jdbc.update("update bookings set refund_status='CREATING',refund_requested_at=now()-interval '2 days'");
  var adminSession=admin.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)adminSession.path("user")).put("role","ADMIN");
  Path config=Files.createTempFile("eema-admin-browser-",".json");try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(Map.of("booking",b,"otherBooking",ids.get(1),"accounts",Map.of("ADMIN",adminSession,"SCOUT",scout))));var process=new ProcessBuilder("node","scripts/qa/admin-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}finally{Files.deleteIfExists(config);}verifyNoInteractions(mollie);
 }
}
