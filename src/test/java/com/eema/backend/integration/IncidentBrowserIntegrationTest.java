package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="eema.inc.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18089")
class IncidentBrowserIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Test void realIncidentWorkflow()throws Exception{
  UUID b=create(venue(owner,60),scout,"09:00","10:00");String before=row(b);long notices=notices(b);
  jdbc.update("update users set email_verified=false where id=?",id(scout));
  jdbc.update("delete from policy_acceptances where user_id=?",id(scout));
  // Registration originally supplied SCOUT in this fixture; expose the authoritative changed role to UI.
  var adminSession=admin.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)adminSession.path("user")).put("role","ADMIN");
  Map<String,Object> fixture=Map.of("booking",b,"accounts",Map.of("SCOUT",scout,"OWNER",owner,"ADMIN",adminSession,"OTHER",second));
  Path config=Files.createTempFile("eema-inc-browser-",".json");
  try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(fixture));var process=new ProcessBuilder("node","scripts/qa/inc-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}finally{Files.deleteIfExists(config);}
  assertEquals(before,row(b));assertEquals(notices,notices(b));verifyNoInteractions(mollie);
  assertEquals(1,rows("incident_reports","booking_id",b));assertEquals("CLOSED",jdbc.queryForObject("select status from incident_reports where booking_id=?",String.class,b));
 }
}
