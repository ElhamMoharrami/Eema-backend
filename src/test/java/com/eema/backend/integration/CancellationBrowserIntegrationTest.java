package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="eema.cancellation.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18086")
class CancellationBrowserIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Test void cancellationBrowserJourney() throws Exception {
        var fixture=new HashMap<String,Object>();
        for(String name:List.of("success","zero","session","expiry")) {
            UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
            String pid="tr_"+b.toString().replace("-","");
            jdbc.update("update bookings set mollie_payment_id=? where id=?",pid,b);
            var amount=new MolliePaymentResponse.Amount("125.50","EUR");
            bookings.processPaidPayment(new MolliePaymentResponse(pid,"paid",amount,null,Map.of("bookingId",b.toString())));
            if(name.equals("zero"))jdbc.update("update bookings set grace_period_ends_at=now()-interval '2 days',full_refund_until=now()-interval '2 days',partial_refund_until=now()-interval '1 day' where id=?",b);
            when(mollie.createRefund(eq(pid),eq(b),any(),any())).thenReturn(new MollieRefundResponse("re_"+b,"pending",pid,amount));
            fixture.put(name,b);
        }
        fixture.put("accounts",Map.of("SCOUT",call("login",Map.of("email",email(scout),"password",QA_PASSWORD),200),
                "SECOND",call("login",Map.of("email",email(second),"password",QA_PASSWORD),200)));
        Path config=Files.createTempFile("eema-cancellation-browser-",".json");
        try {
            Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            Files.writeString(config,json.writeValueAsString(fixture));
            var process=new ProcessBuilder("node","scripts/qa/cancellation-browser.cjs",config.toString()).inheritIO().start();
            assertTrue(process.waitFor(200,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());
        } finally {Files.deleteIfExists(config);}
    }
}
