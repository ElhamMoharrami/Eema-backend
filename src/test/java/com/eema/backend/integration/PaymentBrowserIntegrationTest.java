package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="eema.payment.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties="server.port=8080")
class PaymentBrowserIntegrationTest extends InquirySupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Test void browserJourney()throws Exception{
  checkoutExercised=true;var fixture=new HashMap<String,Object>();for(String name:List.of("pending","stale","late","unrelated")){UUID v=venue(owner,60),b=create(v,name.equals("unrelated")?second:scout,"09:00","10:00");action(b,"accept",owner,200);fixture.put(name,b);String pid="tr_"+b.toString().replace("-","");var amount=new MolliePaymentResponse.Amount("125.50","EUR");var links=new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/pay/"+b));when(mollie.createPayment(eq(b),any(),any())).thenReturn(new MolliePaymentResponse(pid,"open",amount,links,Map.of("bookingId",b.toString())));when(mollie.getPayment(pid)).thenReturn(new MolliePaymentResponse(pid,"paid",amount,links,Map.of("bookingId",b.toString())));if(name.equals("late")){req(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/bookings/"+b+"/checkout"),scout,null,201);var refund=new MollieRefundResponse("re_"+b,"queued",pid,amount);when(mollie.createRefund(eq(pid),eq(b),any(),any())).thenReturn(refund);when(mollie.getRefund(eq(pid),anyString())).thenReturn(refund);}
  if(name.equals("late")||name.equals("stale"))jdbc.update("update bookings set payment_expires_at=? where id=?",OffsetDateTime.now(clock).minusSeconds(1),b);
  }
  fixture.put("probe",Boolean.getBoolean("eema.payment.probe"));fixture.put("accounts",Map.of("SCOUT",call("login",Map.of("email",email(scout),"password",QA_PASSWORD),200)));Path config=Files.createTempFile("eema-payment-browser-",".json");try{Files.setPosixFilePermissions(config,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));Files.writeString(config,json.writeValueAsString(fixture));var process=new ProcessBuilder("node","scripts/qa/payment-browser.cjs",config.toString()).inheritIO().start();assertTrue(process.waitFor(200,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}finally{Files.deleteIfExists(config);}
 }
}
