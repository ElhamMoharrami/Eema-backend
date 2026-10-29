package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="eema.edge.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18090")
@Import(EdgeBrowserIntegrationTest.ClockControl.class)
class EdgeBrowserIntegrationTest extends InquirySupport {
 static volatile Instant deadline;
 @org.springframework.boot.test.context.TestComponent
 @RestController static class ClockControl {
  private final TestClock clock;
  ClockControl(TestClock clock){this.clock=clock;}
  @PostMapping("/qa/edge/deadline") ResponseEntity<Void> advance(){clock.now=Objects.requireNonNull(deadline);return ResponseEntity.noContent().build();}
 }
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Test void deadlineAndDstJourneys()throws Exception {
  date=LocalDate.of(2027,3,28);clock.now=Instant.parse("2027-03-14T00:00:00Z");UUID v=venue(owner,1);
  jdbc.update("update venues set name='QA EDGE Browser Studio' where id=?",v);jdbc.update("update venue_operating_hours set start_time='00:00',end_time='06:00' where venue_id=?",v);
  UUID b=create(v,scout,"03:00","04:00");deadline=Instant.parse(detail(b).path("ownerResponseExpiresAt").asText());
  var accounts=new HashMap<String,Object>();for(var a:List.of(owner,scout))accounts.put(a==owner?"OWNER":"SCOUT",call("login",Map.of("email",email(a),"password",QA_PASSWORD),200));
  Path config=Files.createTempFile("eema-edge-browser-",".json");
  try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(Map.of("accounts",accounts,"bookingId",b,"venueId",v,"date",date.toString())));var process=new ProcessBuilder("node","scripts/qa/edge-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}
  finally{Files.deleteIfExists(config);deadline=null;}
  assertEquals("EXPIRED",state(b));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_EXPIRED"),audit(b));assertEquals(0,rows("availability_inquiries","venue_id",v));verifyNoInteractions(mollie);
 }
}
