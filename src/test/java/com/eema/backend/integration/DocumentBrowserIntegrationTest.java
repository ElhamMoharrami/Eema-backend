package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="eema.doc.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18089")
class DocumentBrowserIntegrationTest extends BookingCoreSupport {
 static final Path ROOT=root();static Path root(){try{return Files.createTempDirectory("eema-doc-browser-store-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void store(DynamicPropertyRegistry r){r.add("app.documents.storage-dir",()->ROOT.toString());}
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Autowired com.eema.backend.service.FinancialDocumentService documents;
 @Autowired com.eema.backend.service.PrivateDocumentStorage privateStorage;
 UUID paid()throws Exception{UUID b=create(venue(owner,60),scout,"09:00","10:00");jdbc.update("update bookings set status='CONFIRMED',paid_at=?,confirmed_at=?,total_paid_eur=venue_price_eur where id=?",OffsetDateTime.now(clock),OffsetDateTime.now(clock),b);return b;}
 @Test void realDocumentDownloads()throws Exception{
  UUID b=paid(),missing=paid(),unpaid=create(venue(owner,60),scout,"09:00","10:00"),p=UUID.randomUUID();
  jdbc.update("insert into owner_payouts(id,booking_id,owner_id,venue_id,gross_retained_amount_eur,commission_eur,provider_cost_eur,payout_amount_eur,status,eligible_at,paid_at,external_transfer_reference) select ?,id,?,venue_id,venue_price_eur,commission_eur,1.50,venue_price_eur-commission_eur-1.50,'PAID',now(),now(),'QA-browser-transfer' from bookings where id=?",p,id(owner),b);
  documents.bookingReceipt(missing,users.findById(id(scout)).orElseThrow());String ref=jdbc.queryForObject("select storage_reference from financial_documents where booking_id=?",String.class,missing);privateStorage.delete(ref);
  Map<String,Object> fixture=Map.of("booking",b,"payout",p,"missing",missing,"unpaid",unpaid,"accounts",Map.of("SCOUT",scout,"OWNER",owner,"OTHER",second));
  Path config=Files.createTempFile("eema-doc-browser-",".json");
  try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(fixture));var process=new ProcessBuilder("node","scripts/qa/doc-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}finally{Files.deleteIfExists(config);try(var files=Files.walk(ROOT)){for(var f:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}}
 }
}
