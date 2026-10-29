package com.eema.backend.integration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@EnabledIfSystemProperty(named="eema.dest.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18088")
class DestinationBrowserIntegrationTest extends BookingCoreSupport {
    @DynamicPropertySource static void crypto(DynamicPropertyRegistry r){DestinationIntegrationTest.cryptoConfig(r);}
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Test void destinationBrowserJourney()throws Exception{
        for(var a:List.of(owner,other))jdbc.update("insert into owner_verifications(owner_id,status,verification_method,submitted_at,reviewed_at,reviewed_by) values (?,'VERIFIED','MANUAL_REVIEW',now(),now(),?)",id(a),id(admin));
        var alternateAdmin=actor("SCOUT");jdbc.update("update users set role='ADMIN' where id=?",id(alternateAdmin));
        var fixture=new HashMap<String,Object>();
        fixture.put("pending",uuid(req(post("/api/owner/payout-destinations"),other,Map.of("accountHolderName","QA Other Bank Holder","iban",DestinationIntegrationTest.IBAN_B),200)));
        fixture.put("alternate",uuid(req(post("/api/owner/payout-destinations"),other,Map.of("accountHolderName","QA Alternate Bank Holder","iban",DestinationIntegrationTest.IBAN_C),200)));
        fixture.put("accounts",Map.of("ADMIN",call("login",Map.of("email",email(admin),"password",QA_PASSWORD),200),"ADMIN2",call("login",Map.of("email",email(alternateAdmin),"password",QA_PASSWORD),200),"OWNER",call("login",Map.of("email",email(owner),"password",QA_PASSWORD),200),"OTHER",call("login",Map.of("email",email(other),"password",QA_PASSWORD),200)));
        Path config=Files.createTempFile("eema-dest-browser-",".json");
        try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(fixture));var process=new ProcessBuilder("node","scripts/qa/dest-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(240,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}finally{Files.deleteIfExists(config);}
    }
}
