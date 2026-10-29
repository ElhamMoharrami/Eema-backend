package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.service.OwnerPayoutService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import java.math.BigDecimal;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Coherent local journeys: actual HTTP/security/transactions, PostgreSQL and private files; fake provider. */
class JourneyIntegrationTest extends InquirySupport {
 static final Path ROOT=root();
 static Path root(){try{return Files.createTempDirectory("eema-journey-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void config(DynamicPropertyRegistry r){
  r.add("app.documents.storage-dir",()->ROOT.resolve("documents").toString());
  r.add("app.storage.upload-dir",()->ROOT.resolve("uploads").toString());
  r.add("app.payout-destination.enabled",()->true);
  r.add("app.payout-destination.encryption-key",()->Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
  r.add("app.payout-destination.fingerprint-key",()->Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes()));
 }
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @BeforeEach void providerScope(){checkoutExercised=true;}
 @AfterAll static void cleanup()throws Exception{try(var files=Files.walk(ROOT)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 @Autowired OwnerPayoutService payouts;
 JsonNode onboard(String role)throws Exception{
  var a=actor(role);assertFalse(a.path("user").path("emailVerified").asBoolean());
  call("verify-email",Map.of("token",verifier(id(a))),200);acceptPolicies(a);
  assertTrue(req(get("/api/auth/me"),a,null,200).path("emailVerified").asBoolean());return a;
 }
 String pid(UUID b){return "tr_"+b.toString().replace("-","");}
 MolliePaymentResponse payment(UUID b,String status,String amount){return new MolliePaymentResponse(pid(b),status,new MolliePaymentResponse.Amount(amount,"EUR"),new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/qa")),Map.of("bookingId",b.toString()));}
 void paid(UUID b,JsonNode a)throws Exception{
  String amount=jdbc.queryForObject("select venue_price_eur::text from bookings where id=?",String.class,b);
  when(mollie.createPayment(eq(b),eq(new BigDecimal(amount)),any())).thenReturn(payment(b,"open",amount));
  req(post("/api/bookings/"+b+"/checkout"),a,null,201);
  when(mollie.getPayment(pid(b))).thenReturn(payment(b,"paid",amount));
  for(int i=0;i<2;i++)assertEquals(200,mvc.perform(post("/api/webhooks/mollie/payment").contextPath("/api").contentType("application/x-www-form-urlencoded").param("id",pid(b))).andReturn().getResponse().getStatus());
  assertEquals("CONFIRMED",state(b));assertEquals(1,Collections.frequency(audit(b),"BOOKING_CONFIRMED"));assertEquals(1,rows("owner_payouts","booking_id",b));verify(mollie,times(1)).createPayment(eq(b),any(),any());
 }
 byte[] pdf(String path,JsonNode actor,int expected)throws Exception{
  var r=mvc.perform(get(path).contextPath("/api").header("Authorization","Bearer "+access(actor))).andReturn().getResponse();assertEquals(expected,r.getStatus());
  if(expected==200){assertEquals("application/pdf",r.getContentType());assertTrue(r.getHeader("Cache-Control").contains("no-store"));assertTrue(r.getContentAsByteArray().length>100);}return r.getContentAsByteArray();
 }
 UUID confirmed()throws Exception{UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);paid(b,scout);return b;}
 UUID activeVenue(JsonNode o)throws Exception{
  UUID verification=uuid(req(post("/api/owner/verification/submit"),o,Map.of("note","QA synthetic owner"),200));
  req(post("/api/admin/owner-verifications/"+verification+"/approve"),admin,Map.of("verificationMethod","MANUAL_REVIEW"),200);
  var input=new HashMap<String,Object>(Map.of("name","QA Journey Studio","neighbourhood","QA Journey "+UUID.randomUUID(),"sizeM2",100,"hourlyRateEur",new BigDecimal("125.50"),"minimumBookingMinutes",60,"minimumLeadTimeMinutes",60,"tags",List.of()));
  UUID v=uuid(req(post("/api/venues"),o,input,201));assertEquals("DRAFT",req(get("/api/venues/"+v),o,null,200).path("status").asText());
  var dto=req(get("/api/venues/"+v),o,null,200);var update=new HashMap<String,Object>();for(var c:UpdateVenueRequest.class.getRecordComponents())update.put(c.getName(),json.convertValue(dto.get(c.getName()),Object.class));
  update.put("maxCrewSize",20);update.put("permittedActivities",List.of("FILM","PHOTO"));update.put("productionRules","QA production rules");update.put("timezone","Europe/Amsterdam");
  for(String f:List.of("naturalLight","blackoutPossible","loadingAccess","parkingAvailable","liftAvailable","wheelchairAccessible","bathroomsAvailable","soundRecordingSuitable"))update.put(f,true);
  req(put("/api/venues/"+v),o,update,200);
  var image=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
  for(int i=0;i<2;i++)req(multipart("/api/venues/"+v+"/photos").file(new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/png",bytes.toByteArray())),o,null,200);
  req(put("/api/venues/"+v+"/availability/weekly"),o,Map.of("intervals",List.of(Map.of("dayOfWeek",date.getDayOfWeek().name(),"startTime","07:00","endTime","20:00"))),200);
  req(post("/api/venues/"+v+"/submit-for-review"),o,null,200);req(post("/api/admin/venues/"+v+"/approve"),admin,Map.of(),200);return v;
 }
 @Test void scoutAndOwnerOnboardingThroughReceiptAndRecordedPayout()throws Exception{
  var o=onboard("OWNER");var s=onboard("SCOUT");UUID v=activeVenue(o);
  UUID dest=uuid(req(post("/api/owner/payout-destinations"),o,Map.of("accountHolderName","QA Journey Owner","iban",DestinationIntegrationTest.IBAN_A),200));req(post("/api/admin/payout-destinations/"+dest+"/verify"),admin,null,200);
  req(get("/api/venues/"+v),null,null,200);available(v);
  UUID b=create(v,s,"09:15","13:45");assertEquals("REQUESTED",state(b));Object policySnapshot=references(b);assertEquals(4,((List<?>)policySnapshot).size());
  assertEquals(new BigDecimal("564.75"),jdbc.queryForObject("select venue_price_eur from bookings where id=?",BigDecimal.class,b));
  action(b,"accept",o,200);assertEquals("AWAITING_PAYMENT",state(b));paid(b,s);
  String receipt="/api/bookings/"+b+"/receipt";byte[] first=pdf(receipt,s,200);pdf(receipt,second,403);assertArrayEquals(first,pdf(receipt,s,200));
  UUID p=jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b);assertNotEquals("PAID",payouts.get(p).status().name());pdf("/api/owner/payouts/"+p+"/statement",o,409);
  bookings.reconcileMollieFee(b,new BigDecimal("1.23"));clock.now=date.plusDays(1).atStartOfDay(ZoneId.of("Europe/Amsterdam")).toInstant();payouts.evaluateReadiness(p);assertEquals("READY",payouts.get(p).status().name());
  req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference","QA separately simulated transfer"),200);
  assertEquals("PAID",req(get("/api/owner/payouts/"+p),o,null,200).path("status").asText());pdf("/api/owner/payouts/"+p+"/statement",o,200);assertEquals(policySnapshot,references(b));assertEquals(1,Collections.frequency(audit(b),"PAYOUT_MARKED_PAID"));
 }
 @Test void inquiryAcceptanceThroughConfirmationDoesNotExpandCalendar()throws Exception{
  UUID v=inquiryVenue();var before=calendar(v);UUID i=open(v,scout);offer(i,owner,date,"18:00","20:00",200);var accepted=inquiryAction(i,"accept",scout,200);
  UUID b=uuid(accepted);assertEquals("ACCEPTED",inquiryState(i));assertEquals("AWAITING_PAYMENT",state(b));paid(b,scout);assertEquals(before,calendar(v));assertEquals(1,jdbc.queryForObject("select count(*) from availability_inquiries where accepted_booking_id=?",Integer.class,b));
 }
 @Test void competingRequestsKeepWinnerAndDenyStaleLoserCheckout()throws Exception{
  UUID v=venue(owner,60),a=create(v,scout,"09:00","10:00"),b=create(v,second,"09:00","10:00");action(a,"accept",owner,200);assertEquals("UNAVAILABLE",state(b));long count=notices(b);action(b,"accept",owner,409);req(post("/api/bookings/"+b+"/checkout"),second,null,400);assertEquals(count,notices(b));assertEquals("UNAVAILABLE",req(get("/api/bookings/"+b),second,null,200).path("status").asText());paid(a,scout);verify(mollie,never()).createPayment(eq(b),any(),any());
 }
 @Test void suspensionPreservesPaidHistoryAndRejectsOldRequestUntilReactivation()throws Exception{
  var o=onboard("OWNER");UUID v=activeVenue(o),b=create(v,scout,"09:00","10:00"),pending=create(v,scout,"11:00","12:00");action(b,"accept",o,200);paid(b,scout);String before=row(b);
  req(post("/api/admin/venues/"+v+"/suspend"),admin,Map.of("reason","QA review"),200);req(get("/api/venues/"+v),null,null,403);action(pending,"accept",o,409);req(post("/api/bookings"),scout,body(v,"13:00","14:00"),404);assertEquals(before,row(b));pdf("/api/bookings/"+b+"/receipt",scout,200);
  req(post("/api/admin/venues/"+v+"/reactivate"),admin,null,200);action(pending,"accept",o,200);assertEquals("AWAITING_PAYMENT",state(pending));
 }
 @Test void passwordRecoveryRetainsBookingAndEstablishedJwtSemantics()throws Exception{
  var s=onboard("SCOUT");UUID b=create(venue(owner,60),s,"09:00","10:00");action(b,"accept",owner,200);String before=row(b);
  call("forgot-password",Map.of("email",email(s)),200);call("reset-password",Map.of("token",resetter(id(s)),"newPassword","Journey-new-password!"),200);call("refresh",Map.of("refreshToken",refresh(s)),401);req(get("/api/auth/me"),s,null,200);assertEquals(before,row(b));
  call("login",Map.of("email",email(s),"password",QA_PASSWORD),401);var recovered=call("login",Map.of("email",email(s),"password","Journey-new-password!"),200);paid(b,recovered);
 }
 @Test void policyReplacementPreservesPaidReceiptAndResumesNewCommitment()throws Exception{
  UUID old=confirmed();Object snapshots=references(old);String path="/api/bookings/"+old+"/receipt";byte[] receipt=pdf(path,scout,200);
  var policy=req(post("/api/admin/policies"),admin,Map.of("policyType","TERMS_OF_SERVICE","version","QA-JOURNEY-"+UUID.randomUUID(),"title","QA NOT LEGAL TERMS","content","QA NOT LEGAL TERMS","effectiveAt",OffsetDateTime.now().toString()),201);req(post("/api/admin/policies/"+uuid(policy)+"/publish"),admin,null,200);
  try{req(post("/api/bookings"),scout,body(venue(owner,60),"09:00","10:00"),403);assertEquals(snapshots,references(old));assertArrayEquals(receipt,pdf(path,scout,200));}
  finally{policies.clear();policies.addAll(jdbc.queryForList("select id from policy_versions where status='PUBLISHED'",UUID.class));for(var a:List.of(owner,other,scout,second))acceptPolicies(a);}
  UUID next=create(venue(owner,60),scout,"09:00","10:00");assertNotEquals(snapshots,references(next));action(next,"accept",owner,200);paid(next,scout);
 }
 @ParameterizedTest @CsvSource({"SCOUT,FULL,125.50","SCOUT,PARTIAL,62.75","SCOUT,ZERO,0.00","OWNER,FULL,125.50","ADMIN,FULL,125.50"})
 void cancellationJourney(String role,String tier,String amount)throws Exception{
  UUID b=confirmed();var actor=role.equals("OWNER")?owner:role.equals("ADMIN")?admin:scout;
  if(tier.equals("PARTIAL"))clock.now=jdbc.queryForObject("select full_refund_until from bookings where id=?",OffsetDateTime.class,b).toInstant().plusSeconds(1);
  if(tier.equals("ZERO"))clock.now=jdbc.queryForObject("select partial_refund_until from bookings where id=?",OffsetDateTime.class,b).toInstant().plusSeconds(1);
  var preview=req(get("/api/bookings/"+b+"/cancellation-preview"),actor,null,200);assertEquals(0,new BigDecimal(amount).compareTo(preview.path("refundAmountEur").decimalValue()));
  var refund=new MollieRefundResponse("re_"+b.toString().replace("-",""),"refunded",pid(b),new MolliePaymentResponse.Amount(amount,"EUR"));when(mollie.createRefund(eq(pid(b)),eq(b),any(),any())).thenReturn(refund);when(mollie.getRefund(eq(pid(b)),any())).thenReturn(refund);
  req(post("/api/bookings/"+b+"/cancel"),actor,Map.of("reason","QA cancellation","previewToken",preview.path("previewToken").asText()),200);assertEquals("CANCELLED",state(b));assertEquals(tier.equals("ZERO")?"NONE":"REFUNDED",detail(b).path("refundStatus").asText());
  assertEquals(0,new BigDecimal(amount).compareTo(detail(b).path("cancellationRefundAmountEur").decimalValue()));assertEquals(role.equals("ADMIN")?"EEMA":role.equals("OWNER")?"OWNER":"OWNER_SETTLEMENT",jdbc.queryForObject("select provider_cost_responsibility from bookings where id=?",String.class,b));
  UUID payoutId=jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b);var ledger=payouts.get(payoutId);BigDecimal retained=new BigDecimal("125.50").subtract(new BigDecimal(amount));
  assertEquals(retained,ledger.grossRetainedAmountEur());assertEquals(retained.multiply(new BigDecimal("0.20")).setScale(2),ledger.commissionEur());assertEquals(retained.multiply(new BigDecimal("0.80")).setScale(2),ledger.payoutAmountEur());
  req(get("/api/bookings/"+b),owner,null,200);pdf("/api/bookings/"+b+"/receipt",scout,200);if(tier.equals("ZERO"))verify(mollie,never()).createRefund(eq(pid(b)),eq(b),any(),any());else verify(mollie,times(1)).createRefund(eq(pid(b)),eq(b),eq(new BigDecimal(amount)),any());
 }
 @Test void adminIncidentAndLiabilityOperationsStayAuditedAndScoped()throws Exception{
  UUID b=confirmed();for(String path:List.of("owner-verifications","venues","payout-destinations","bookings","incidents","payouts","liabilities","policies","operations/exceptions"))req(get("/api/admin/"+path),admin,null,200);
  req(get("/api/admin/bookings/"+b),admin,null,200);req(get("/api/admin/bookings/"+b+"/events"),admin,null,200);
  UUID i=uuid(req(post("/api/bookings/"+b+"/incidents"),scout,Map.of("category","OTHER","description","QA participant report"),201));String before=row(b);
  req(post("/api/admin/incidents/"+i+"/start-review"),admin,null,200);req(post("/api/admin/incidents/"+i+"/resolve"),admin,Map.of("resolutionNote","QA internal decision"),200);req(post("/api/admin/incidents/"+i+"/close"),admin,null,200);assertEquals(before,row(b));assertEquals(4,rows("marketplace_trust_events","subject_id",i));assertFalse(req(get("/api/bookings/"+b+"/incidents"),scout,null,200).toString().contains("QA internal decision"));
  // Separate existing OPEN liability fixture; incident handling must not invent financial adjustments.
  UUID l=UUID.randomUUID();jdbc.update("insert into owner_financial_liabilities(id,owner_id,booking_id,type,amount_eur,status) values (?,?,?,'PROVIDER_COST',1.23,'OPEN')",l,id(owner),b);
  req(post("/api/admin/liabilities/"+l+"/settle"),owner,null,403);req(post("/api/admin/liabilities/"+l+"/settle"),admin,null,200);assertEquals("SETTLED",jdbc.queryForObject("select status from owner_financial_liabilities where id=?",String.class,l));
 }
}
