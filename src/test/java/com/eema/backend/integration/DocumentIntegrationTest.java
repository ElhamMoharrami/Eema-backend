package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@org.springframework.context.annotation.Import(DocumentIntegrationTest.Faults.class)
class DocumentIntegrationTest extends BookingCoreSupport {
 static final Path ROOT=createRoot();
 static Path createRoot(){try{return Files.createTempDirectory("eema-doc-it-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void storage(DynamicPropertyRegistry r){r.add("app.documents.storage-dir",()->ROOT.toString());}
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @AfterAll static void cleanup()throws Exception{try(var paths=Files.walk(ROOT)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 static volatile String fault;
 static volatile UUID raceId;
 static volatile CyclicBarrier barrier;
 static final AtomicInteger arrivals=new AtomicInteger();
 static final List<String> stored=new CopyOnWriteArrayList<>(),deleted=new CopyOnWriteArrayList<>();
 @org.springframework.boot.test.context.TestConfiguration static class Faults {
  @org.springframework.context.annotation.Bean static org.springframework.beans.factory.config.BeanPostProcessor documentFaults(){return new org.springframework.beans.factory.config.BeanPostProcessor(){public Object postProcessAfterInitialization(Object bean,String name){
   if(!(bean instanceof PrivateDocumentStorage)&&!(bean instanceof FinancialDocumentRepository)&&!(bean instanceof BookingRepository)&&!(bean instanceof OwnerPayoutRepository))return bean;
   var proxy=new org.springframework.aop.framework.ProxyFactory(bean);
   proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) call->{String method=call.getMethod().getName();
    if((bean instanceof BookingRepository||bean instanceof OwnerPayoutRepository)&&method.equals("findByIdForUpdate")&&Objects.equals(raceId,call.getArguments()[0])&&arrivals.incrementAndGet()<=2)barrier.await(15,TimeUnit.SECONDS);
    if(bean instanceof PrivateDocumentStorage&&method.equals("delete"))deleted.add((String)call.getArguments()[0]);
    if(bean instanceof FinancialDocumentRepository&&method.equals("saveAndFlush")&&"database".equals(fault))((FinancialDocument)call.getArguments()[0]).setTemplateVersion(0);
    if(bean instanceof PrivateDocumentStorage&&method.equals("store")){
     if("storage".equals(fault))throw new IllegalStateException("QA private store unavailable");
     Object result=call.proceed();stored.add((String)call.getArguments()[0]);
     if("commit".equals(fault))TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void beforeCommit(boolean readOnly){throw new IllegalStateException("QA commit failure after private store");}});
     return result;
    }
    return call.proceed();
   });return proxy.getProxy();
  }};}
 }
 @AfterEach void reset(){fault=null;raceId=null;barrier=null;verifyNoInteractions(mollie);}
 @Autowired OwnerPayoutRepository payoutRepository;
 UUID paidBooking()throws Exception{
  UUID b=create(venue(owner,60),scout,"09:00","10:00");
  jdbc.update("update bookings set status='CONFIRMED',paid_at=?,confirmed_at=?,total_paid_eur=venue_price_eur,mollie_payment_id=? where id=?",OffsetDateTime.now(clock),OffsetDateTime.now(clock),"tr_doc_"+b,b);return b;
 }
 UUID paidPayout(UUID b){
  UUID v=jdbc.queryForObject("select venue_id from bookings where id=?",UUID.class,b),destination=UUID.randomUUID();
  jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by,replaced_at) values (?,?,'IBAN','EUR','QA Document Bank','qa-opaque',?,'4821','REPLACED',now(),now(),?,now())",destination,id(owner),destination.toString(),id(admin));
  return payoutRepository.saveAndFlush(OwnerPayout.builder().bookingId(b).ownerId(id(owner)).venueId(v).grossRetainedAmountEur(new BigDecimal("125.50")).commissionEur(new BigDecimal("25.10")).providerCostEur(new BigDecimal("1.50")).payoutAmountEur(new BigDecimal("98.90")).status(OwnerPayoutStatus.PAID).eligibleAt(OffsetDateTime.now(clock)).paidAt(OffsetDateTime.now(clock)).externalTransferReference("QA-transfer-record").payoutDestination(com.eema.backend.entity.OwnerPayoutDestination.builder().id(destination).build()).build()).getId();
 }
 String receipt(UUID b){return "/api/bookings/"+b+"/receipt";}
 String statement(UUID p){return "/api/owner/payouts/"+p+"/statement";}
 MockHttpServletResponse download(String path,JsonNode actor,int expected)throws Exception{
  var request=get(path).contextPath("/api");if(actor!=null)request.header("Authorization","Bearer "+access(actor));var response=mvc.perform(request).andReturn().getResponse();assertEquals(expected,response.getStatus(),expected==200?"Expected PDF":response.getContentAsString());
  if(expected==200){assertEquals("application/pdf",response.getContentType());assertTrue(response.getHeader("Content-Disposition").startsWith("attachment;"));assertTrue(response.getHeader("Cache-Control").contains("no-store"));assertEquals(response.getContentAsByteArray().length,response.getContentLength());assertTrue(new String(response.getContentAsByteArray(),0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"));}
  else assertNotEquals("application/pdf",response.getContentType());return response;
 }
 String text(byte[] bytes)throws Exception{try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)){return new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);}}
 long docs(UUID b){return rows("financial_documents","booking_id",b);}
 Path storedFile(UUID b){return ROOT.resolve(jdbc.queryForObject("select storage_reference from financial_documents where booking_id=?",String.class,b));}
 @ParameterizedTest @EnumSource(value=BookingStatus.class,names={"CONFIRMED","CANCELLED","REFUNDED"})
 void eligiblePaidHistoryIssuesExactlyOnce(BookingStatus status)throws Exception{
  UUID b=paidBooking();jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);long notices=notices(b);String before=row(b);var first=download(receipt(b),scout,200);var repeat=download(receipt(b),admin,200);assertArrayEquals(first.getContentAsByteArray(),repeat.getContentAsByteArray());assertEquals(1,docs(b));assertEquals(1,Collections.frequency(audit(b),"BOOKING_RECEIPT_ISSUED"));assertEquals(before,row(b));assertEquals(notices,notices(b));assertTrue(text(first.getContentAsByteArray()).contains(status.name()));
 }
 @ParameterizedTest @EnumSource(value=BookingStatus.class,names={"REQUESTED","AWAITING_PAYMENT","EXPIRED","UNAVAILABLE","DECLINED"})
 void ineligibleBookingStatesNeverIssue(BookingStatus status)throws Exception{
  UUID b=paidBooking();jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);String before=row(b);var audit=audit(b);int stores=stored.size();download(receipt(b),scout,409);assertEquals(0,docs(b));assertEquals(before,row(b));assertEquals(audit,audit(b));assertEquals(stores,stored.size());
 }
 @ParameterizedTest @ValueSource(strings={"paid_at=null","confirmed_at=null","total_paid_eur=0","late_payment_detected_at=now()"})
 void missingPaidConfirmationAndLatePaymentsNeverIssue(String mutation)throws Exception{UUID b=paidBooking();jdbc.update("update bookings set "+mutation+" where id=?",b);download(receipt(b),scout,409);assertEquals(0,docs(b));}
 @Test void receiptAuthorizationAlsoAppliesToExistingBytes()throws Exception{
  UUID b=paidBooking();for(var actor:List.of(second,owner,other))download(receipt(b),actor,403);download(receipt(b),null,401);assertEquals(0,docs(b));byte[] issued=download(receipt(b),scout,200).getContentAsByteArray();for(var actor:List.of(second,owner,other))download(receipt(b),actor,403);download(receipt(b),null,401);assertArrayEquals(issued,download(receipt(b),admin,200).getContentAsByteArray());assertEquals(1,docs(b));
 }
 @Test void receiptPreservesFinancialSnapshotsAndImmutableBytes()throws Exception{
  UUID b=paidBooking();byte[] bytes=download(receipt(b),scout,200).getContentAsByteArray();String text=text(bytes);for(String expected:List.of("RCT-",b.toString(),"125.50","60 minutes","QA Supplied Contact","booking-contact@example.test","Europe/Amsterdam","not presented as a tax invoice"))assertTrue(text.contains(expected),expected);
  for(String hidden:List.of("commission","Provider cost","Owner payout","tr_doc_","iban","liabilit"))assertFalse(text.toLowerCase().contains(hidden.toLowerCase()),hidden);
  jdbc.update("update venues set name='QA changed venue',hourly_rate_eur=777,neighbourhood='QA changed area' where id=(select venue_id from bookings where id=?)",b);jdbc.update("update bookings set status='CANCELLED' where id=?",b);assertArrayEquals(bytes,download(receipt(b),scout,200).getContentAsByteArray());assertArrayEquals(bytes,Files.readAllBytes(storedFile(b)));
 }
 @Test void historicalReadDoesNotRequireCurrentPoliciesOrVerifiedEmail()throws Exception{
  UUID b=paidBooking();jdbc.update("update users set email_verified=false where id=?",id(scout));jdbc.update("delete from policy_acceptances where user_id=?",id(scout));try{download(receipt(b),scout,200);}finally{jdbc.update("update users set email_verified=true where id=?",id(scout));acceptPolicies(scout);}
 }
 @ParameterizedTest @EnumSource(value=OwnerPayoutStatus.class,names={"CALCULATED","HELD","READY","CANCELLED"})
 void nonPaidPayoutCannotIssue(OwnerPayoutStatus status)throws Exception{UUID b=paidBooking(),p=paidPayout(b);jdbc.update("update owner_payouts set status=? where id=?",status.name(),p);download(statement(p),owner,409);assertEquals(0,docs(b));}
 @Test void legacyPaidStatementWithMissingReferenceIsRejected()throws Exception{UUID b=paidBooking(),p=paidPayout(b);jdbc.update("update owner_payouts set external_transfer_reference=null where id=?",p);download(statement(p),owner,409);assertEquals(0,docs(b));}
 @Test void statementUsesHistoricalRoutingLedgerAndPrivateOwnership()throws Exception{
  UUID b=paidBooking(),p=paidPayout(b);for(var actor:List.of(other,scout,second))download(statement(p),actor,403);download(statement(p),null,401);assertEquals(0,docs(b));byte[] bytes=download(statement(p),owner,200).getContentAsByteArray();String text=text(bytes);for(String expected:List.of("PST-",p.toString(),b.toString(),"QA-transfer-record","4821","125.50","25.10","1.50","98.90","not presented as a tax invoice"))assertTrue(text.contains(expected),expected);assertFalse(text.contains("qa-opaque"));assertFalse(text.toLowerCase().contains("liabilit"));
  jdbc.update("update users set full_name='QA owner renamed' where id=?",id(owner));jdbc.update("update venues set name='QA new venue name' where id=(select venue_id from bookings where id=?)",b);
  assertArrayEquals(bytes,download(statement(p),admin,200).getContentAsByteArray());assertEquals(1,docs(b));assertEquals(1,Collections.frequency(audit(b),"OWNER_PAYOUT_STATEMENT_ISSUED"));for(var actor:List.of(other,scout))download(statement(p),actor,403);
 }
 @RepeatedTest(3) void simultaneousReceiptRequestsIssueOneObjectAndAudit()throws Exception{UUID b=paidBooking();raceDownloads(b,receipt(b),scout);assertEquals(1,docs(b));assertEquals(1,Collections.frequency(audit(b),"BOOKING_RECEIPT_ISSUED"));}
 @RepeatedTest(3) void simultaneousStatementRequestsIssueOneObjectAndAudit()throws Exception{UUID b=paidBooking(),p=paidPayout(b);raceDownloads(p,statement(p),owner);assertEquals(1,docs(b));assertEquals(1,Collections.frequency(audit(b),"OWNER_PAYOUT_STATEMENT_ISSUED"));}
 void raceDownloads(UUID id,String path,JsonNode actor)throws Exception{raceId=id;barrier=new CyclicBarrier(2);arrivals.set(0);int before=stored.size();var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->download(path,actor,200).getContentAsByteArray());var b=pool.submit(()->download(path,actor,200).getContentAsByteArray());assertArrayEquals(a.get(25,TimeUnit.SECONDS),b.get(25,TimeUnit.SECONDS));assertEquals(before+1,stored.size());}finally{raceId=null;pool.shutdownNow();}}
 @ParameterizedTest @ValueSource(strings={"storage","database","commit"})
 void failedIssuanceNeverSucceedsAndCleansStoredObject(String failure)throws Exception{
  UUID b=paidBooking();var beforeAudit=audit(b);int before=stored.size();fault=failure;download(receipt(b),scout,500);fault=null;assertEquals(0,docs(b));assertEquals(beforeAudit,audit(b));
  if(!failure.equals("storage")){assertEquals(before+1,stored.size());String ref=stored.get(stored.size()-1);assertTrue(deleted.contains(ref),"Rollback must attempt private object cleanup");assertFalse(Files.exists(ROOT.resolve(ref)));}
  download(receipt(b),scout,200);assertEquals(1,docs(b));
 }
 @Test void missingIssuedObjectFailsWithoutRegenerating()throws Exception{
  UUID b=paidBooking();download(receipt(b),scout,200);Path file=storedFile(b);Files.delete(file);int before=stored.size();var audit=audit(b);download(receipt(b),scout,500);assertEquals(before,stored.size());assertEquals(1,docs(b));assertEquals(audit,audit(b));assertFalse(Files.exists(file));
 }
 @Test void storedDocumentsAreNotPublicUploadResources()throws Exception{
  download("/api/fonts/NotoSans-Regular.ttf",scout,404);
  UUID b=paidBooking();download(receipt(b),scout,200);String ref=jdbc.queryForObject("select storage_reference from financial_documents where booking_id=?",String.class,b);download("/api/upload/files/"+Path.of(ref).getFileName(),null,404);download("/api/"+ref,null,401);
 }
 @Test void uniqueIdentityAndForeignKeysAreDatabaseEnforced()throws Exception{
  UUID b=paidBooking();download(receipt(b),scout,200);assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into financial_documents select ?,document_type,booking_id,owner_payout_id,recipient_user_id,?,status,template_version,?,issued_at,created_at from financial_documents where booking_id=?",UUID.randomUUID(),"RCT-QA-duplicate","booking-receipts/duplicate.pdf",b));assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update financial_documents set recipient_user_id=? where booking_id=?",UUID.randomUUID(),b));assertEquals(1,docs(b));
 }
 @ParameterizedTest @ValueSource(strings={"José García","Zoë François","Daniël Müller","Łukasz Żółć","Νίκος","Алексей","李雷","محمد","李雷 García"})
 void unicodeReceiptKeepsOriginalNameMoneyAndImmutableBytes(String name)throws Exception{
  UUID b=paidBooking();jdbc.update("update bookings set scout_contact_name=? where id=?",name,b);
  byte[] bytes=download(receipt(b),scout,200).getContentAsByteArray();String body=text(bytes);
  assertTrue(body.contains(name));assertTrue(body.contains("125.50"));assertFalse(body.contains("Provider cost"));assertFalse(body.contains("commission"));
  jdbc.update("update bookings set scout_contact_name=? where id=?","Unsupported 🎬",b);
  assertArrayEquals(bytes,download(receipt(b),scout,200).getContentAsByteArray());download(receipt(b),second,403);assertEquals(1,docs(b));
 }
 @ParameterizedTest @ValueSource(strings={"李雷 García","محمد","Łukasz Żółć"})
 void unicodeStatementKeepsOriginalNameLedgerMaskAndImmutableBytes(String name)throws Exception{
  UUID b=paidBooking(),p=paidPayout(b);String previous=jdbc.queryForObject("select full_name from users where id=?",String.class,id(owner));
  try{jdbc.update("update users set full_name=? where id=?",name,id(owner));byte[] bytes=download(statement(p),owner,200).getContentAsByteArray();String body=text(bytes);
   for(String expected:List.of(name,"125.50","25.10","1.50","98.90","••••••••••4821"))assertTrue(body.contains(expected),expected);
   assertFalse(body.contains("qa-opaque"));jdbc.update("update users set full_name=? where id=?","Unsupported 🎬",id(owner));assertArrayEquals(bytes,download(statement(p),owner,200).getContentAsByteArray());download(statement(p),other,403);
  }finally{jdbc.update("update users set full_name=? where id=?",previous,id(owner));}
 }
 @ParameterizedTest @ValueSource(strings={"receipt","statement"})
 void unsupportedNameRollsBackWithoutStorageOrAuditAndRetryWorks(String kind)throws Exception{
  UUID b=paidBooking(),p=kind.equals("statement")?paidPayout(b):null;String previous=jdbc.queryForObject("select full_name from users where id=?",String.class,id(owner));
  String path=p==null?receipt(b):statement(p);var actor=p==null?scout:owner;var beforeAudit=audit(b);int before=stored.size();long beforeNotices=notices(b);
  try{if(p==null)jdbc.update("update bookings set scout_contact_name=? where id=?","Unknown \u0378",b);else jdbc.update("update users set full_name=? where id=?","Unknown \u0378",id(owner));
   var response=download(path,actor,500);assertFalse(response.getContentAsString().contains("Unknown"));assertEquals(0,docs(b));assertEquals(before,stored.size());assertEquals(beforeAudit,audit(b));assertEquals(beforeNotices,notices(b));
   if(p==null)jdbc.update("update bookings set scout_contact_name=? where id=?","李雷",b);else jdbc.update("update users set full_name=? where id=?","李雷",id(owner));download(path,actor,200);assertEquals(1,docs(b));
  }finally{jdbc.update("update users set full_name=? where id=?",previous,id(owner));}
 }

}
