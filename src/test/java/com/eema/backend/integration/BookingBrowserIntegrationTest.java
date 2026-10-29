package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="eema.booking.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=8080")
class BookingBrowserIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void freshBrowserAccounts(){owner=null;policies.clear();}
    @Test void browserJourney()throws Exception{
        UUID v=venue(owner,60);jdbc.update("update venues set name='QA Browser Booking Studio' where id=?",v);
        UUID staleDecline=create(venue(owner,60),scout,"09:00","12:00"),staleAccept=create(venue(owner,60),scout,"09:00","12:00");
        var realNow=clock.now;clock.now=date.atTime(6,0).atZone(java.time.ZoneId.of("Europe/Amsterdam")).toInstant();
        UUID leadExpired=create(venue(owner,60),scout,"09:00","12:00");clock.now=date.atTime(8,30).atZone(java.time.ZoneId.of("Europe/Amsterdam")).toInstant();action(leadExpired,"accept",owner,409);clock.now=realNow;
        var accounts=new HashMap<String,Object>();for(var a:List.of(owner,scout))accounts.put(a==owner?"OWNER":"SCOUT",call("login",Map.of("email",email(a),"password",QA_PASSWORD),200));
        Path config=Files.createTempFile("eema-booking-browser-",".json");
        try{
            Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(Map.of("accounts",accounts,"venueId",v,"date",date.toString(),"leadExpired",leadExpired,"staleDecline",staleDecline,"staleAccept",staleAccept,"probe",Boolean.getBoolean("eema.booking.probe"))));
            var process=new ProcessBuilder("node","scripts/qa/booking-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(180,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());org.mockito.Mockito.verifyNoInteractions(mollie);
            assertEquals(0,jdbc.queryForObject("select count(*) from bookings where mollie_payment_id is not null",Integer.class));
        }finally{Files.deleteIfExists(config);}
    }
}
