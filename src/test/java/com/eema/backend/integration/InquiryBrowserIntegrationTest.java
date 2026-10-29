package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="eema.inquiry.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=8080")
class InquiryBrowserIntegrationTest extends InquirySupport {
    @BeforeAll static void freshBrowserAccounts(){owner=null;policies.clear();}
    @Test void browserJourney()throws Exception{
        UUID v=inquiryVenue();jdbc.update("update venues set name='QA Browser Inquiry Studio' where id=?",v);
        jdbc.update("insert into venue_availability_overrides(id,venue_id,override_date,override_type,all_day) values (?,?,?,'BLOCKED',true)",UUID.randomUUID(),v,date);
        var fixture=new HashMap<String,Object>();
        for(String name:List.of("expired","staleOffer","staleDecline","scoutConflict","scoutOccupied","ownerBlocked","decline")){
            UUID venue=inquiryVenue();jdbc.update("update venues set name=? where id=?","QA "+name,venue);UUID i=open(venue,scout);fixture.put(name,i);
            if(name.equals("expired")){var now=clock.now;clock.now=now.minus(java.time.Duration.ofHours(49));offer(i,owner,date,"18:00","20:00",200);clock.now=now;inquiryService.expireOffers(java.time.OffsetDateTime.ofInstant(now,java.time.ZoneOffset.UTC));}
            if(name.equals("scoutConflict")||name.equals("scoutOccupied"))offer(i,owner,date,"18:00","20:00",200);
        }
        var accounts=new HashMap<String,Object>();for(var a:List.of(owner,scout))accounts.put(a==owner?"OWNER":"SCOUT",call("login",Map.of("email",email(a),"password",QA_PASSWORD),200));
        fixture.put("accounts",accounts);fixture.put("venueId",v);fixture.put("date",date.toString());fixture.put("offeredDate",date.plusDays(1).toString());fixture.put("probe",Boolean.getBoolean("eema.inquiry.probe"));
        Path config=Files.createTempFile("eema-inquiry-browser-",".json");
        try{
            Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(fixture));
            var process=new ProcessBuilder("node","scripts/qa/inquiry-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());
            assertEquals(0,jdbc.queryForObject("select count(*) from bookings where mollie_payment_id is not null",Integer.class));
        }finally{Files.deleteIfExists(config);}
    }
}
