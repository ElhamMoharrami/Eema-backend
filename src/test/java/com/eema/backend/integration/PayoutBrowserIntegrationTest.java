package com.eema.backend.integration;
import com.eema.backend.dto.response.*;
import com.eema.backend.service.OwnerPayoutService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="eema.payout.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=18087")
class PayoutBrowserIntegrationTest extends BookingCoreSupport {
    @Autowired OwnerPayoutService payouts;
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Test void payoutBrowserJourney()throws Exception{
        var fixture=new HashMap<String,Object>();UUID destination=UUID.randomUUID();
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",destination,id(owner),destination.toString(),id(admin));
        for(String name:List.of("success","conflict","conflict2","held")){
            UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);String pid="tr_"+b.toString().replace("-","");
            jdbc.update("update bookings set mollie_payment_id=? where id=?",pid,b);
            bookings.processPaidPayment(new MolliePaymentResponse(pid,"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
            if(!name.equals("held")){bookings.reconcileMollieFee(b,new BigDecimal("1.23"));jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now().minusDays(1),b);payouts.recalculateForBooking(b);}
            fixture.put(name,jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b));
            if(name.equals("success")){UUID l=UUID.randomUUID();jdbc.update("insert into owner_financial_liabilities(id,owner_id,booking_id,type,amount_eur,status) values (?,?,?,'PROVIDER_COST',1.23,'OPEN')",l,id(owner),b);fixture.put("liability",l);}
        }
        fixture.put("accounts",Map.of("ADMIN",call("login",Map.of("email",email(admin),"password",QA_PASSWORD),200),"OWNER",call("login",Map.of("email",email(owner),"password",QA_PASSWORD),200),"OTHER",call("login",Map.of("email",email(other),"password",QA_PASSWORD),200)));
        Path config=Files.createTempFile("eema-payout-browser-",".json");
        try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(fixture));var process=new ProcessBuilder("node","scripts/qa/payout-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(220,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}finally{Files.deleteIfExists(config);}
    }
}
