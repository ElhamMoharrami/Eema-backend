package com.eema.backend.integration;

import com.eema.backend.entity.enums.*;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.math.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@org.springframework.context.annotation.Import(PriceIntegrationTest.Races.class)
class PriceIntegrationTest extends InquirySupport {
 static volatile java.util.concurrent.CountDownLatch priceRead, priceResume;
 @org.springframework.boot.test.context.TestConfiguration static class Races {
  @org.springframework.context.annotation.Bean static org.springframework.beans.factory.config.BeanPostProcessor pricePause(){return new org.springframework.beans.factory.config.BeanPostProcessor(){public Object postProcessAfterInitialization(Object bean,String name){if(!(bean instanceof com.eema.backend.service.BookingPriceCalculator))return bean;var p=new org.springframework.aop.framework.ProxyFactory(bean);p.setProxyTargetClass(true);p.addAdvice((org.aopalliance.intercept.MethodInterceptor) call->{if(priceRead!=null&&call.getMethod().getName().equals("calculate")){priceRead.countDown();assertTrue(priceResume.await(15,java.util.concurrent.TimeUnit.SECONDS));}return call.proceed();});return p.getProxy();}};}
 }
 @AfterEach void resetPricePause(){if(priceResume!=null)priceResume.countDown();priceRead=null;priceResume=null;}

 @BeforeAll static void fresh(){owner=null;policies.clear();}
 Map<String,Object> configuration(String rate,int minimum,String cap){var b=new HashMap<String,Object>(Map.of("name","QA Price Studio","neighbourhood","QA Price","sizeM2",100,"hourlyRateEur",new BigDecimal(rate),"minimumBookingMinutes",minimum,"minimumLeadTimeMinutes",60,"tags",List.of()));b.put("fullDayRateEur",cap==null?null:new BigDecimal(cap));return b;}
 Map<String,Object> updateBody(UUID v)throws Exception{return json.convertValue(req(get("/api/venues/"+v),owner,null,200),Map.class);}
 void pricing(UUID v,String rate,int minimum,String cap)throws Exception{var b=updateBody(v);b.put("hourlyRateEur",new BigDecimal(rate));b.put("minimumBookingMinutes",minimum);b.put("fullDayRateEur",cap==null?null:new BigDecimal(cap));req(put("/api/venues/"+v),owner,b,200);}
 @Test void fractionalCentResponseMatchesPersistedConfiguration()throws Exception{
  var result=req(post("/api/venues"),owner,configuration("19.999",1,"50.005"),201);UUID v=uuid(result);var reloaded=req(get("/api/venues/"+v),owner,null,200);
  assertEquals(0,reloaded.path("hourlyRateEur").decimalValue().compareTo(result.path("hourlyRateEur").decimalValue()));assertEquals(0,reloaded.path("fullDayRateEur").decimalValue().compareTo(result.path("fullDayRateEur").decimalValue()));
  var update=updateBody(v);update.put("hourlyRateEur",new BigDecimal("29.995"));update.put("fullDayRateEur",new BigDecimal("60.005"));var saved=req(put("/api/venues/"+v),owner,update,200);var again=req(get("/api/venues/"+v),owner,null,200);for(String field:List.of("hourlyRateEur","fullDayRateEur"))assertEquals(saved.get(field),again.get(field));

 }
 @Test void calculatedOverflowIsSafeAndLeavesNoBooking()throws Exception{
  UUID v=inquiryVenue();pricing(v,"99999999.99",10080,null);int notices=hooks.committedBookings.size();req(post("/api/bookings"),scout,body(v,"09:00","10:00"),400);assertEquals(0,rows("bookings","venue_id",v));assertEquals(notices,hooks.committedBookings.size());
 } @ParameterizedTest @ValueSource(strings={"PRICE-001","PRICE-006","PRICE-007","PRICE-010"})
 void persistedSnapshotAndMaliciousCheckoutUseOriginalPrice(String id)throws Exception{
  JsonNode n=null;for(var row:json.readTree(getClass().getResourceAsStream("/pricing-cases.json")))if(row.path("id").asText().equals(id))n=row;assertNotNull(n);
  UUID v=inquiryVenue();pricing(v,n.path("hourly").asText(),n.path("minimum").asInt(),n.path("cap").isNull()?null:n.path("cap").asText());String end=LocalTime.of(9,0).plusMinutes(n.path("duration").asInt()).toString();UUID b=create(v,scout,"09:00",end);
  var before=detail(b);BigDecimal price=new BigDecimal(n.path("expected").asText());assertEquals(0,price.compareTo(before.path("venuePriceEur").decimalValue()));assertEquals(n.path("duration").asInt(),before.path("durationMinutes").asInt());assertEquals(n.path("billable").asInt(),before.path("billableMinutes").asInt());assertEquals(0,new BigDecimal(n.path("hourly").asText()).compareTo(before.path("hourlyRateEurSnapshot").decimalValue()));assertEquals(n.path("minimum").asInt(),before.path("minimumBookingMinutesSnapshot").asInt());
  if(n.path("cap").isNull())assertTrue(before.path("fullDayRateEurSnapshot").isNull());else assertEquals(0,new BigDecimal(n.path("cap").asText()).compareTo(before.path("fullDayRateEurSnapshot").decimalValue()));
  assertEquals(0,price.compareTo(jdbc.queryForObject("select venue_price_eur from bookings where id=?",BigDecimal.class,b)));
  pricing(v,"777.77",600,"9999");action(b,"accept",owner,200);var after=detail(b);for(String f:List.of("venuePriceEur","hourlyRateEurSnapshot","minimumBookingMinutesSnapshot","fullDayRateEurSnapshot","durationMinutes","billableMinutes","commissionPercentage","commissionEur"))assertEquals(before.get(f),after.get(f));
  checkoutExercised=true;when(mollie.createPayment(eq(b),eq(price),any())).thenReturn(new MolliePaymentResponse("tr_"+b,"open",new MolliePaymentResponse.Amount(price.toPlainString(),"EUR"),new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/qa")),Map.of("bookingId",b.toString())));
  req(post("/api/bookings/"+b+"/checkout"),scout,Map.of("amount","0.01","price",1,"venuePriceEur",0,"total",2),201);verify(mollie).createPayment(eq(b),eq(price),any());
  assertEquals(0,new com.eema.backend.service.BookingFinancials().commission(price,before.path("commissionPercentage").decimalValue()).compareTo(before.path("commissionEur").decimalValue()));
 }
 @Test void inquiryPriceIsCapturedAtAcceptanceNotOffer()throws Exception{
  UUID v=inquiryVenue();pricing(v,"100",60,null);UUID i=open(v,scout);offer(i,owner,date,"18:00","20:00",200);pricing(v,"19.99",1,null);UUID b=uuid(inquiryAction(i,"accept",scout,200));assertEquals(0,new BigDecimal("39.98").compareTo(detail(b).path("venuePriceEur").decimalValue()));pricing(v,"999",240,"1000");assertEquals(0,new BigDecimal("39.98").compareTo(detail(b).path("venuePriceEur").decimalValue()));
 }
 @Test void zeroPriceIsRejectedBeforeBookingPersistence()throws Exception{
  UUID v=inquiryVenue();pricing(v,"0.01",1,null);var error=req(post("/api/bookings"),scout,body(v,"09:00","09:01"),400);assertTrue(error.path("message").asText().contains("minimum bookable amount"));assertEquals(0,rows("bookings","venue_id",v));verifyNoInteractions(mollie);
 }
 @ParameterizedTest @ValueSource(strings={"hourlyRateEur","minimumBookingMinutes","fullDayRateEur"})
 void pricingHttpValidationAndDatabaseConstraints(String field)throws Exception{
  for(int value:List.of(0,-1)){var body=configuration("100",60,null);body.put(field,value);req(post("/api/venues"),owner,body,400);}
  var large=configuration("100",60,null);large.put(field,field.equals("minimumBookingMinutes")?10081:new BigDecimal("100000000"));req(post("/api/venues"),owner,large,400);
  UUID v=inquiryVenue();String column=switch(field){case "hourlyRateEur"->"hourly_rate_eur";case "fullDayRateEur"->"full_day_rate_eur";default->"minimum_booking_minutes";};assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update venues set "+column+"=0 where id=?",v));
 }
 @Test void databaseMoneyScaleAndRequiredSnapshots()throws Exception{
  var c=jdbc.queryForMap("select numeric_precision,numeric_scale,is_nullable from information_schema.columns where table_name='bookings' and column_name='venue_price_eur'");assertEquals(10,((Number)c.get("numeric_precision")).intValue());assertEquals(2,((Number)c.get("numeric_scale")).intValue());assertEquals("NO",c.get("is_nullable"));UUID v=inquiryVenue(),b=create(v,scout,"09:00","10:00");for(String f:List.of("hourly_rate_eur_snapshot","minimum_booking_minutes_snapshot","duration_minutes","billable_minutes"))assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set "+f+"=null where id=?",b));jdbc.update("update bookings set full_day_rate_eur_snapshot=null where id=?",b);
 }

 @RepeatedTest(3) void normalRequestKeepsCoherentOldPriceAcrossConcurrentOwnerEdit()throws Exception{
  UUID v=inquiryVenue();pricing(v,"100",60,"500");priceRead=new java.util.concurrent.CountDownLatch(1);priceResume=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
  try{var result=pool.submit(()->create(v,scout,"09:00","10:30"));assertTrue(priceRead.await(10,java.util.concurrent.TimeUnit.SECONDS));pricing(v,"200",120,"300");priceResume.countDown();var dto=detail(result.get(20,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,new BigDecimal("100").compareTo(dto.path("hourlyRateEurSnapshot").decimalValue()));assertEquals(60,dto.path("minimumBookingMinutesSnapshot").asInt());assertEquals(0,new BigDecimal("500").compareTo(dto.path("fullDayRateEurSnapshot").decimalValue()));assertEquals(0,new BigDecimal("150").compareTo(dto.path("venuePriceEur").decimalValue()));}finally{priceResume.countDown();pool.shutdownNow();}
 }
 @RepeatedTest(3) void inquiryConversionAndOwnerEditChooseOneWholePricingConfiguration()throws Exception{
  UUID v=inquiryVenue();pricing(v,"100",60,"500");UUID i=open(v,scout);offer(i,owner,date,"18:00","19:30",200);var update=updateBody(v);update.put("hourlyRateEur",200);update.put("minimumBookingMinutes",120);update.put("fullDayRateEur",300);gate=new Gate("venue",Set.of(v));var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{var accept=pool.submit(()->inquiryAction(i,"accept",scout,200));var edit=pool.submit(()->req(put("/api/venues/"+v),owner,update,200));var dto=accept.get(20,java.util.concurrent.TimeUnit.SECONDS);edit.get(20,java.util.concurrent.TimeUnit.SECONDS);boolean old=dto.path("hourlyRateEurSnapshot").decimalValue().compareTo(new BigDecimal("100"))==0;assertEquals(old?60:120,dto.path("minimumBookingMinutesSnapshot").asInt());assertEquals(0,new BigDecimal(old?"500":"300").compareTo(dto.path("fullDayRateEurSnapshot").decimalValue()));assertEquals(0,new BigDecimal(old?"150":"300").compareTo(dto.path("venuePriceEur").decimalValue()));}finally{gate=null;pool.shutdownNow();}
 }

}
