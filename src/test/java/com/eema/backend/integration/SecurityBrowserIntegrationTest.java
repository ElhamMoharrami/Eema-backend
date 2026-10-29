package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@EnabledIfSystemProperty(named="eema.sec.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18090")
class SecurityBrowserIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Test void malformedSessionsEscapedContentAndTamperedRole()throws Exception{
  String payload="<script>window.__qaXss=1</script>";
  req(post("/api/admin/policies"),admin,Map.of("policyType","TERMS_OF_SERVICE","version","SEC-UI-"+UUID.randomUUID(),"title",payload,"content",payload,"effectiveAt",OffsetDateTime.now().toString()),201);
  var adminSession=admin.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)adminSession.path("user")).put("role","ADMIN");
  Path config=Files.createTempFile("eema-sec-browser-",".json");
  try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(Map.of("accounts",Map.of("ADMIN",adminSession,"SCOUT",scout),"policyText",payload)));var process=new ProcessBuilder("node","scripts/qa/sec-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}
  finally{Files.deleteIfExists(config);}verifyNoInteractions(mollie);
 }
}
