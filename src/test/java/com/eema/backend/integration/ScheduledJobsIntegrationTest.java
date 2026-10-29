package com.eema.backend.integration;

import com.eema.backend.config.AppProperties;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual scheduled entrypoints, deterministic clocks/barriers, disposable PostgreSQL only. */
class ScheduledJobsIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @MockBean(name="taskScheduler") TaskScheduler scheduler;
    @MockBean MollieFeeReportingClient reporting;
    @Autowired BookingApprovalExpirationJob expiry;
    @Autowired AvailabilityInquiryExpirationJob inquiryExpiry;
    @Autowired AuthTokenCleanupJob cleanup;
    @Autowired OwnerPayoutReadinessJob readiness;
    @Autowired OwnerPayoutService payoutService;
    @Autowired BookingRepository repository;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MeterRegistry meters;
    @Autowired WorkflowObservability observability;

    @BeforeEach void isolate(){
        org.springframework.test.util.ReflectionTestUtils.setField(readiness,"cursor",null);
        jdbc.execute("truncate bookings cascade");
        jdbc.execute("truncate availability_inquiries,notification_outbox,email_verification_tokens,password_reset_tokens");
    }
    @AfterEach void noMoneyMovement(){verifyNoInteractions(mollie);}
    OffsetDateTime now(){return OffsetDateTime.now(clock);}
    long count(String table,String condition){return jdbc.queryForObject("select count(*) from "+table+" where "+condition,Long.class);}
    UUID seed()throws Exception{return create(venue(owner,60),scout,"09:00","10:00");}
    UUID cloneBooking(UUID seed,int day,Map<String,Object> overrides)throws Exception{
        UUID id=UUID.randomUUID();var data=new HashMap<String,Object>(overrides);data.put("id",id);data.put("shoot_date",date.plusDays(day));
        jdbc.update("insert into bookings select (jsonb_populate_record(null::bookings,to_jsonb(b)||?::jsonb)).* from bookings b where id=?",json.writeValueAsString(data),seed);return id;
    }
    Map<String,Object> due(String state){return Map.of("status",state,state.equals("REQUESTED")?"owner_response_expires_at":"payment_expires_at",now());}
    void race(Runnable a,Runnable b)throws Exception{
        var start=new CyclicBarrier(2);try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Void> x=()->{start.await(10,TimeUnit.SECONDS);a.run();return null;};
            Callable<Void> y=()->{start.await(10,TimeUnit.SECONDS);b.run();return null;};
            var f=pool.submit(x);var g=pool.submit(y);f.get(30,TimeUnit.SECONDS);g.get(30,TimeUnit.SECONDS);
        }
    }
    @Test void bookingJobProcessesOneHundredPerKindThenRemainderWithoutDuplicateEvents()throws Exception{
        UUID base=seed();double requests=meters.get("eema.workflow.booking.request.expired").counter().count();double approvals=meters.get("eema.workflow.booking.approval.expired").counter().count();
        for(int n=1;n<=101;n++){cloneBooking(base,n,due("REQUESTED"));cloneBooking(base,n+200,due("AWAITING_PAYMENT"));}
        UUID future=cloneBooking(base,500,Map.of("status","AWAITING_PAYMENT","payment_expires_at",now().plusNanos(1000)));
        expiry.expireUnpaidApprovals();assertEquals(200,count("bookings","status='EXPIRED'"));
        expiry.expireUnpaidApprovals();expiry.expireUnpaidApprovals();assertEquals(202,count("bookings","status='EXPIRED'"));
        assertEquals("REQUESTED",state(base));assertEquals("AWAITING_PAYMENT",state(future));
        assertEquals(101,count("booking_events","event_type='REQUEST_EXPIRED'"));assertEquals(101,count("booking_events","event_type='APPROVAL_EXPIRED'"));
        assertEquals(202,count("notification_outbox","notification_type in ('REQUEST_EXPIRED','APPROVAL_EXPIRED')"));
        assertEquals(requests+101,meters.get("eema.workflow.booking.request.expired").counter().count());assertEquals(approvals+101,meters.get("eema.workflow.booking.approval.expired").counter().count());
    }
    @Test void bookingWorkersAndBusyRowLeaveOneCommittedTransitionPerBooking()throws Exception{
        UUID base=seed();var ids=new ArrayList<UUID>();for(int n=1;n<=12;n++)ids.add(cloneBooking(base,n,due(n%2==0?"REQUESTED":"AWAITING_PAYMENT")));
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var pool=Executors.newSingleThreadExecutor()){
            var held=pool.submit(()->new TransactionTemplate(transactions).execute(s->{jdbc.queryForObject("select id from bookings where id=? for update",UUID.class,ids.getFirst());locked.countDown();try{assertTrue(release.await(20,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return null;}));
            try{assertTrue(locked.await(10,TimeUnit.SECONDS));race(expiry::expireUnpaidApprovals,expiry::expireUnpaidApprovals);assertEquals(11,count("bookings","status='EXPIRED'"));}finally{release.countDown();held.get(10,TimeUnit.SECONDS);}
        }
        expiry.expireUnpaidApprovals();assertEquals(12,count("booking_events","event_type in ('REQUEST_EXPIRED','APPROVAL_EXPIRED')"));
    }
    UUID inquiry(UUID venue,OffsetDateTime deadline){UUID id=UUID.randomUUID();jdbc.update("""
        insert into availability_inquiries(id,venue_id,scout_id,requested_date,requested_start_time,requested_end_time,message,status,
         offered_date,offered_start_time,offered_end_time,offer_expires_at,offered_at)
        values(?,?,?,?,'09:00','10:00','QA job offer','OFFERED',?,'09:00','10:00',?,?)
        """,id,venue,id(scout),date,date,deadline,deadline.minusHours(1));return id;}
    @Test void inquiryJobUsesInjectedTimeAndBoundedRepeatedConcurrentBatches()throws Exception{
        UUID v=venue(owner,60);clock.now=clock.now.plus(Duration.ofDays(1));
        for(int n=0;n<101;n++)inquiry(v,now());UUID future=inquiry(v,now().plusNanos(1000));
        inquiryExpiry.expireOffers();assertEquals(100,count("availability_inquiries","status='EXPIRED'"));
        race(inquiryExpiry::expireOffers,inquiryExpiry::expireOffers);assertEquals(101,count("availability_inquiries","status='EXPIRED'"));
        assertEquals("OFFERED",jdbc.queryForObject("select status from availability_inquiries where id=?",String.class,future));
        assertEquals(101,count("notification_outbox","notification_type='OFFER_EXPIRED'"));
    }
    @Test void tokenCleanupUsesInjectedTimeBoundedBatchesAndRetainsEqualityAndSessions()throws Exception{
        clock.now=clock.now.plus(Duration.ofDays(2));long sessions=count("refresh_tokens","true");
        for(String table:List.of("email_verification_tokens","password_reset_tokens")){
            jdbc.update("insert into "+table+"(id,user_id,token_hash,created_at,expires_at) select gen_random_uuid(),?,md5(n::text)||md5(n::text),?,? from generate_series(1,101)n",id(scout),now().minusDays(1),now().minusNanos(1000));
            jdbc.update("insert into "+table+"(id,user_id,token_hash,created_at,expires_at) values(gen_random_uuid(),?,?,?,?),(gen_random_uuid(),?,?,?,?)",id(scout),"a".repeat(64),now().minusDays(1),now(),id(scout),"b".repeat(64),now().minusDays(1),now().plusDays(1));
        }
        cleanup.cleanup();for(String table:List.of("email_verification_tokens","password_reset_tokens"))assertEquals(3,count(table,"true"));
        race(cleanup::cleanup,cleanup::cleanup);for(String table:List.of("email_verification_tokens","password_reset_tokens"))assertEquals(2,count(table,"true"));assertEquals(sessions,count("refresh_tokens","true"));
    }
    MollieFeeReconciliationJob feeJob(){return new MollieFeeReconciliationJob(new AppProperties(null,null,null,new AppProperties.MollieProperties(null,null,null,"QA-disabled-network",true,1000,1000),null,null,null,null,null),repository,reporting,bookings);}
    UUID paidClone(UUID base,int day,OffsetDateTime paid)throws Exception{return cloneBooking(base,day,Map.of("status","CONFIRMED","paid_at",paid,"mollie_payment_id","tr_job_"+UUID.randomUUID(),"total_paid_eur",new BigDecimal("125.50"),"commission_eur",new BigDecimal("25.10")));}
    @Test void feeJobProcessesBoundedSuccessfulBatchesAndIgnoresUnpaidRows()throws Exception{
        UUID base=seed();for(int n=1;n<=101;n++)paidClone(base,n,now());
        when(reporting.findAuthoritativePaymentFee(any())).thenReturn(Optional.of(new BigDecimal("1.25")));var job=feeJob();job.reconcile();assertEquals(100,count("bookings","mollie_fee_reconciled"));job.reconcile();job.reconcile();assertEquals(101,count("bookings","mollie_fee_reconciled"));verify(reporting,times(101)).findAuthoritativePaymentFee(any());assertEquals("REQUESTED",state(base));
    }
    @Test void unavailableOlderFeesMustNotPermanentlyStarveLaterAvailableFee()throws Exception{
        UUID base=seed();for(int n=1;n<=100;n++)paidClone(base,n,now().minusSeconds(200-n));UUID later=paidClone(base,101,now());
        String providerId=jdbc.queryForObject("select mollie_payment_id from bookings where id=?",String.class,later);
        when(reporting.findAuthoritativePaymentFee(any())).thenReturn(Optional.empty());when(reporting.findAuthoritativePaymentFee(providerId)).thenReturn(Optional.of(new BigDecimal("1.25")));
        var job=feeJob();job.reconcile();job.reconcile();job.reconcile();assertTrue(jdbc.queryForObject("select mollie_fee_reconciled from bookings where id=?",Boolean.class,later),"Repeated first-page selection starves the later authoritative fee");
        when(reporting.findAuthoritativePaymentFee(any())).thenReturn(Optional.of(new BigDecimal("1.25")));
        job.reconcile();assertEquals(101,count("bookings","mollie_fee_reconciled"));
    }
    @Test void overlappingFeeWorkersReconcileOneFinancialOutcome()throws Exception{
        UUID base=seed();UUID paid=paidClone(base,1,now());var entered=new CyclicBarrier(2);
        when(reporting.findAuthoritativePaymentFee(any())).thenAnswer(i->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());entered.await(10,TimeUnit.SECONDS);return Optional.of(new BigDecimal("1.25"));});
        race(()->feeJob().reconcile(),()->feeJob().reconcile());assertEquals(1,count("booking_events","event_type='PAYMENT_RECONCILED'"));assertEquals(1,rows("owner_payouts","booking_id",paid));verify(reporting,times(2)).findAuthoritativePaymentFee(any());
    }
    @Test void rollbackDoesNotIncrementExpiryCounterOrRetainOutbox()throws Exception{
        UUID base=seed();UUID due=cloneBooking(base,1,due("REQUESTED"));double before=meters.get("eema.workflow.booking.request.expired").counter().count();
        new TransactionTemplate(transactions).executeWithoutResult(s->{bookings.expireDueRequests(now(),100);s.setRollbackOnly();});
        assertEquals("REQUESTED",state(due));assertEquals(before,meters.get("eema.workflow.booking.request.expired").counter().count());assertEquals(0,rows("notification_outbox","aggregate_id",due));
    }
    void destination(){
        jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where owner_id=? and status='VERIFIED'",id(owner));UUID d=UUID.randomUUID();
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",d,id(owner),d.toString(),id(admin));
    }
    List<UUID> payoutBatch(int held)throws Exception{
        UUID base=seed();destination();var ids=new ArrayList<UUID>();
        for(int n=1;n<=101;n++){
            UUID b=paidClone(base,n,now().minusSeconds(200-n));
            jdbc.update("update bookings set mollie_fee_reconciled=?,mollie_fee_eur=1.25,owner_payout_eur=99.15 where id=?",n>held,b);
            payoutService.recalculateForBooking(b);ids.add(b);
        }
        clock.now=date.plusDays(102).atStartOfDay(ZoneOffset.UTC).toInstant();return ids;
    }
    @Test void payoutReadinessProcessesBoundedBatchesWithoutTransferOrDuplicateAudit()throws Exception{
        var ids=payoutBatch(0);readiness.scan();assertEquals(100,count("owner_payouts","status='READY'"));
        readiness.scan();readiness.scan();assertEquals(101,count("owner_payouts","status='READY'"));assertEquals(101,count("booking_events","event_type='PAYOUT_READY'"));assertEquals(0,count("owner_payouts","status='PAID'"));
    }
    @Test void heldOlderPayoutsMustNotPermanentlyStarveLaterReadyPayout()throws Exception{
        var ids=payoutBatch(100);readiness.scan();readiness.scan();readiness.scan();
        assertEquals("READY",jdbc.queryForObject("select status from owner_payouts where booking_id=?",String.class,ids.getLast()),"Repeated first-page selection starves the later ready payout");
        assertEquals(100,count("owner_payouts","status='HELD'"));
    }
    @Test void allWorkflowGaugesAreCompleteReadOnlyAndConcurrent()throws Exception{
        UUID base=seed();OffsetDateTime old=now().minusDays(10);
        for(int n=1;n<=101;n++)cloneBooking(base,n,Map.of("status","CANCELLED","payment_creation_status","CREATING","payment_creation_started_at",old,"refund_status","PENDING","refund_requested_at",old));
        for(int n=102;n<=303;n++){
            UUID b=cloneBooking(base,n,Map.of("status","CANCELLED"));
            jdbc.update("insert into owner_payouts(booking_id,owner_id,venue_id,gross_retained_amount_eur,commission_eur,provider_cost_eur,payout_amount_eur,status,eligible_at,ready_at) values(?,?,(select venue_id from bookings where id=?),10,1,1,8,?,?,?)",b,id(owner),b,n<=202?"HELD":"READY",old,old);
            if(n<=202)jdbc.update("insert into owner_financial_liabilities(owner_id,booking_id,type,amount_eur,status) values(?,?,'PROVIDER_COST',1,'OPEN')",id(owner),b);
        }
        String before=jdbc.queryForObject("select json_agg(b order by id)::text from bookings b",String.class);
        String payoutBefore=jdbc.queryForObject("select json_agg(p order by id)::text from owner_payouts p",String.class);
        race(observability::scanStuckStates,observability::scanStuckStates);
        for(String metric:List.of("payout.held","payout.ready","payout.overdue","owner.liability.open"))assertEquals(101,meters.get("eema.workflow."+metric).gauge().value(),metric);
        assertEquals(payoutBefore,jdbc.queryForObject("select json_agg(p order by id)::text from owner_payouts p",String.class));
        assertEquals(101,meters.get("eema.workflow.payment.creation.stuck").gauge().value());assertEquals(101,meters.get("eema.workflow.refund.stuck").gauge().value());
        assertEquals(before,jdbc.queryForObject("select json_agg(b order by id)::text from bookings b",String.class));
    }

    @Test void usedTokensAreDeletedBeforeTheirExpiryWithoutTouchingUnusedFutureTokens(){
        for(String table:List.of("email_verification_tokens","password_reset_tokens")){
            jdbc.update("insert into "+table+"(id,user_id,token_hash,created_at,expires_at,used_at) values(gen_random_uuid(),?,?,?, ?,?)",id(scout),"c".repeat(64),now().minusDays(1),now().plusDays(1),now());
            jdbc.update("insert into "+table+"(id,user_id,token_hash,created_at,expires_at) values(gen_random_uuid(),?,?,?,?)",id(scout),"d".repeat(64),now().minusDays(1),now().plusDays(1));
        }
        cleanup.cleanup();for(String table:List.of("email_verification_tokens","password_reset_tokens"))assertEquals(List.of("d".repeat(64)),jdbc.queryForList("select token_hash from "+table,String.class));
    }
    @Test void inquiryWorkerSkipsLockedOfferThenRecoversItWithoutDuplicateIntent()throws Exception{
        UUID v=venue(owner,60),busy=inquiry(v,now()),free=inquiry(v,now());var locked=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var pool=Executors.newSingleThreadExecutor()){
            var held=pool.submit(()->new TransactionTemplate(transactions).execute(s->{jdbc.queryForObject("select id from availability_inquiries where id=? for update",UUID.class,busy);locked.countDown();try{assertTrue(release.await(20,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return null;}));
            try{assertTrue(locked.await(10,TimeUnit.SECONDS));inquiryExpiry.expireOffers();assertEquals(1,count("availability_inquiries","status='EXPIRED'"));assertEquals("EXPIRED",jdbc.queryForObject("select status from availability_inquiries where id=?",String.class,free));}finally{release.countDown();held.get(10,TimeUnit.SECONDS);}
        }
        inquiryExpiry.expireOffers();inquiryExpiry.expireOffers();assertEquals(2,count("notification_outbox","notification_type='OFFER_EXPIRED'"));
    }

    @Test void readinessRollbackPreservesScanPositionAndHasNoAuditOutcome()throws Exception{
        payoutBatch(0);
        new TransactionTemplate(transactions).executeWithoutResult(s->{readiness.scan();s.setRollbackOnly();});
        assertEquals(0,count("owner_payouts","status='READY'"));assertEquals(0,count("booking_events","event_type='PAYOUT_READY'"));
        assertNull(org.springframework.test.util.ReflectionTestUtils.getField(readiness,"cursor"));
        readiness.scan();assertEquals(100,count("owner_payouts","status='READY'"));
    }

}
