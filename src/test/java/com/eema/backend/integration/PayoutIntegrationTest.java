package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** PAYOUT contracts on disposable PostgreSQL. No external transfer is performed. */
class PayoutIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Autowired OwnerPayoutService payouts;
    @Autowired OwnerFinancialLiabilityService liabilities;
    @Autowired BookingRepository repository;
    @Autowired OwnerPayoutReadinessJob readiness;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    UUID confirmed() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
        String pid="tr_"+b.toString().replace("-","");
        jdbc.update("update bookings set mollie_payment_id=? where id=?",pid,b);
        bookings.processPaidPayment(new MolliePaymentResponse(pid,"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
        return b;
    }
    UUID payout(UUID b){return jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b);}
    String payoutRow(UUID p){return jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,p);}
    String payoutStatus(UUID p){return jdbc.queryForObject("select status from owner_payouts where id=?",String.class,p);}
    UUID destination(){
        jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where owner_id=? and status='VERIFIED'",id(owner));
        UUID d=UUID.randomUUID();
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",d,id(owner),d.toString(),id(admin));return d;
    }
    UUID ready(UUID b){
        destination();bookings.reconcileMollieFee(b,new BigDecimal("1.23"));
        // Historical paid booking fixture; request creation itself still uses valid future lead time.
        jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now().minusDays(1),b);
        payouts.recalculateForBooking(b);assertEquals("READY",payoutStatus(payout(b)));return payout(b);
    }
    UUID liability(UUID b,String type){UUID l=UUID.randomUUID();jdbc.update("insert into owner_financial_liabilities(id,owner_id,booking_id,type,amount_eur,status) values (?,?,?,?,1.23,'OPEN')",l,id(owner),b,type);return l;}

    @ParameterizedTest @ValueSource(strings={"payouts","liabilities"})
    void adminListsAcceptOmittedAndPartialFilters(String resource)throws Exception{
        UUID b=confirmed(),p=payout(b);if(resource.equals("liabilities"))liability(b,"PROVIDER_COST");
        assertTrue(req(get("/api/admin/"+resource),admin,null,200).path("content").isArray());
        var filtered=req(get("/api/admin/"+resource).param("bookingId",b.toString()).param("size","500"),admin,null,200);
        assertEquals(1,filtered.path("content").size());assertEquals(100,filtered.path("size").asInt());
        assertEquals(b.toString(),filtered.path("content").get(0).path("bookingId").asText());
        assertEquals(0,req(get("/api/admin/"+resource).param("ownerId",id(other).toString()),admin,null,200).path("totalElements").asInt());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void unpaidBookingsCannotAcquireReconciledFees(boolean cancel)throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","10:00");if(cancel)req(post("/api/bookings/"+b+"/cancel"),scout,null,200);
        String before=row(b);assertThrows(IllegalStateException.class,()->bookings.reconcileMollieFee(b,new BigDecimal("1.23")));
        assertEquals(before,row(b));assertEquals(0,Collections.frequency(audit(b),"PAYMENT_RECONCILED"));verifyNoInteractions(mollie);
    }
    @Test void concurrentRecalculationCreatesOneLedgerAndOneAudit()throws Exception{
        UUID b=confirmed();jdbc.update("delete from owner_payouts where booking_id=?",b);jdbc.update("delete from booking_events where booking_id=? and event_type='PAYOUT_CALCULATED'",b);
        race(()->payouts.recalculateForBooking(b),()->payouts.recalculateForBooking(b));
        assertEquals(1,jdbc.queryForObject("select count(*) from owner_payouts where booking_id=?",Integer.class,b));
        assertEquals(1,Collections.frequency(audit(b),"PAYOUT_CALCULATED"));
        var p=payouts.get(payout(b));assertEquals(new BigDecimal("125.50"),p.grossRetainedAmountEur());assertEquals(new BigDecimal("25.10"),p.commissionEur());assertEquals(new BigDecimal("100.40"),p.payoutAmountEur());assertEquals(OwnerPayoutStatus.HELD,p.status());
        jdbc.update("update venues set hourly_rate_eur=999 where id=?",p.venueId());payouts.recalculateForBooking(b);
        assertEquals(p.payoutAmountEur(),payouts.get(p.id()).payoutAmountEur());
    }
    @Test void feeValidationExactReplayAndCorrectionConflictLeaveOneAudit()throws Exception{
        UUID b=confirmed();String initial=row(b);
        assertThrows(IllegalArgumentException.class,()->bookings.reconcileMollieFee(b,new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class,()->bookings.reconcileMollieFee(b,null));assertEquals(initial,row(b));
        race(()->bookings.reconcileMollieFee(b,new BigDecimal("1.23")),()->bookings.reconcileMollieFee(b,new BigDecimal("1.230")));
        String after=row(b);assertThrows(ConflictException.class,()->bookings.reconcileMollieFee(b,new BigDecimal("1.24")));assertEquals(after,row(b));
        assertEquals(1,Collections.frequency(audit(b),"PAYMENT_RECONCILED"));assertEquals(new BigDecimal("99.17"),payouts.get(payout(b)).payoutAmountEur());verifyNoInteractions(mollie);
    }
    @Autowired OwnerPayoutDestinationService destinationService;
    @Test void firstReadyDestinationSurvivesRealReplacementAndPaidRecalculation()throws Exception{
        UUID b=confirmed(),p=ready(b),a=payouts.get(p).payoutDestinationId(),replacement=UUID.randomUUID();
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at) values (?,?,'IBAN','EUR','QA Replacement','qa-encrypted',?,'5678','PENDING_VERIFICATION',now())",replacement,id(owner),replacement.toString());
        payouts.evaluateReadiness(p);assertEquals(a,payouts.get(p).payoutDestinationId());
        destinationService.verify(replacement,"QA reviewed",users.findById(id(admin)).orElseThrow());payouts.evaluateReadiness(p);
        assertEquals("REPLACED",jdbc.queryForObject("select status from owner_payout_destinations where id=?",String.class,a));assertEquals(a,payouts.get(p).payoutDestinationId());
        assertEquals(1,jdbc.queryForObject("select count(*) from marketplace_trust_events where subject_id=? and event_type='PAYOUT_DESTINATION_SNAPSHOTTED'",Integer.class,a));
        UUID future=confirmed();bookings.reconcileMollieFee(future,new BigDecimal("1.23"));
        jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now().minusDays(1),future);
        payouts.recalculateForBooking(future);assertEquals(replacement,payouts.get(payout(future)).payoutDestinationId());
        assertEquals(a,payouts.get(p).payoutDestinationId());
        req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference","QA original destination"),200);
        String snapshot=payoutRow(p);payouts.recalculateForBooking(b);assertEquals(snapshot,payoutRow(p));assertEquals(a,payouts.get(p).payoutDestinationId());
        var own=req(get("/api/owner/payouts/"+p),owner,null,200);assertFalse(own.has("externalTransferReference"));assertFalse(own.has("ibanEncrypted"));assertTrue(own.path("payoutDestinationMaskedIban").asText().endsWith("1234"));
    }
    @Test void payoutReadsAndMutationsRespectRolesAndObjectScope()throws Exception{
        UUID b=confirmed(),p=ready(b);String before=payoutRow(p);
        for(var actor:List.of(owner,other,scout)){
            req(get("/api/admin/payouts"),actor,null,403);req(get("/api/admin/payouts/"+p),actor,null,403);
            req(post("/api/admin/payouts/"+p+"/mark-paid"),actor,Map.of("externalTransferReference","QA denied"),403);
        }
        req(get("/api/owner/payouts/"+p),other,null,403);req(get("/api/owner/payouts/"+p),scout,null,403);
        req(get("/api/admin/payouts/"+p),null,null,401);
        assertEquals(0,req(get("/api/owner/payouts"),other,null,200).path("totalElements").asInt());assertEquals(before,payoutRow(p));verifyNoInteractions(mollie);
    }
    @ParameterizedTest @ValueSource(strings={"","   ","TOO_LONG"})
    void invalidTransferReferenceIsRejectedBeforeMutation(String reference)throws Exception{
        UUID b=confirmed(),p=ready(b);String before=payoutRow(p);if(reference.equals("TOO_LONG"))reference="x".repeat(201);
        req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference",reference),400);assertEquals(before,payoutRow(p));
    }
    @ParameterizedTest @ValueSource(strings={"HELD","CALCULATED","CANCELLED"})
    void onlyReadyCanBeRecordedPaid(String status)throws Exception{
        UUID b=confirmed(),p=payout(b);jdbc.update("update owner_payouts set status=? where id=?",status,p);String before=payoutRow(p);
        req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference","QA conflict"),409);assertEquals(before,payoutRow(p));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void concurrentPaidRecordingIsIdempotentOrConflicts(boolean same)throws Exception{
        UUID b=confirmed(),p=ready(b);var administrator=users.findById(id(admin)).orElseThrow();var winners=new java.util.concurrent.atomic.AtomicInteger();var conflicts=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<String> record=reference->{try{payouts.markPaid(p,reference,administrator);winners.incrementAndGet();}catch(ConflictException e){conflicts.incrementAndGet();}};
        race(()->record.accept(" QA transfer "),()->record.accept(same?"QA transfer":"QA different"));
        assertEquals(same?2:1,winners.get());assertEquals(same?0:1,conflicts.get());assertEquals("PAID",payoutStatus(p));assertEquals(1,Collections.frequency(audit(b),"PAYOUT_MARKED_PAID"));
        String before=payoutRow(p);String reference=payouts.get(p).externalTransferReference();req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference",reference,"payoutAmountEur",999999),200);assertEquals(before,payoutRow(p));verifyNoInteractions(mollie);
    }
    @ParameterizedTest @ValueSource(strings={"PROVIDER_COST","OWNER_PAYOUT_RECOVERY"})
    void liabilitySettlementUsesStoredAmountAndSerializes(String type)throws Exception{
        UUID b=confirmed(),l=liability(b,type);String bookingBefore=row(b),payoutBefore=payoutRow(payout(b));
        for(var actor:List.of(owner,scout)){req(get("/api/admin/liabilities/"+l),actor,null,403);req(post("/api/admin/liabilities/"+l+"/settle"),actor,Map.of("amountEur",999),403);}
        var administrator=users.findById(id(admin)).orElseThrow();race(()->liabilities.settle(l,administrator),()->liabilities.settle(l,administrator));
        var result=req(post("/api/admin/liabilities/"+l+"/settle"),admin,Map.of("amountEur",999),200);assertEquals("SETTLED",result.path("status").asText());assertEquals(new BigDecimal("1.23"),result.path("amountEur").decimalValue());
        assertEquals(1,Collections.frequency(audit(b),"OWNER_LIABILITY_SETTLED"));assertEquals(bookingBefore,row(b));assertEquals(payoutBefore,payoutRow(payout(b)));verifyNoInteractions(mollie);
    }
    @Test void waivedLiabilityCannotBeSettledAndDatabaseRejectsInvalidMoneyAndDuplicates()throws Exception{
        UUID b=confirmed(),l=liability(b,"PROVIDER_COST"),p=payout(b);jdbc.update("update owner_financial_liabilities set status='WAIVED' where id=?",l);
        req(post("/api/admin/liabilities/"+l+"/settle"),admin,null,409);assertEquals("WAIVED",liabilities.get(l).status().name());assertEquals(0,Collections.frequency(audit(b),"OWNER_LIABILITY_SETTLED"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update owner_financial_liabilities set amount_eur=0 where id=?",l));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->liability(b,"PROVIDER_COST"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update owner_payouts set payout_amount_eur=-0.01 where id=?",p));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("delete from bookings where id=?",b));
    }
    @Test void paidRecordingRollsBackWithItsAudit()throws Exception{
        UUID b=confirmed(),p=ready(b);String before=payoutRow(p);var tx=new TransactionTemplate(transactions);
        tx.execute(status->{payouts.markPaid(p,"QA rolled back",users.findById(id(admin)).orElseThrow());status.setRollbackOnly();return null;});
        assertEquals(before,payoutRow(p));assertEquals(0,Collections.frequency(audit(b),"PAYOUT_MARKED_PAID"));
    }
    @Test void readinessWorkerSkipsBusyBookingAndProcessesItOnNextScan()throws Exception{
        UUID b=confirmed();destination();bookings.reconcileMollieFee(b,new BigDecimal("1.23"));
        jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now().minusDays(1),b);
        jdbc.update("update owner_payouts set eligible_at=now()-interval '1 day' where booking_id=?",b);
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);var tx=new TransactionTemplate(transactions);
        try{
            var holder=pool.submit(()->tx.execute(status->{repository.findByIdForUpdate(b).orElseThrow();locked.countDown();try{assertTrue(release.await(15,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return null;}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));var scan=pool.submit(()->readiness.scan());
            try{scan.get(5,TimeUnit.SECONDS);assertEquals("CALCULATED",payoutStatus(payout(b)));}finally{release.countDown();holder.get(10,TimeUnit.SECONDS);scan.get(10,TimeUnit.SECONDS);}
            readiness.scan();assertEquals("READY",payoutStatus(payout(b)));
        }finally{release.countDown();pool.shutdownNow();}
    }
    void race(Runnable a,Runnable b)throws Exception{
        var barrier=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        try{var first=pool.submit(()->{barrier.await(10,TimeUnit.SECONDS);a.run();return true;});var second=pool.submit(()->{barrier.await(10,TimeUnit.SECONDS);b.run();return true;});first.get(20,TimeUnit.SECONDS);second.get(20,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
    }
    @Test void missingOrUnverifiedSnapshotCannotBeRecordedPaid()throws Exception{
        UUID b=confirmed(),p=ready(b),d=payouts.get(p).payoutDestinationId();
        jdbc.update("update owner_payouts set payout_destination_id=null where id=?",p);
        req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference","QA missing"),409);
        jdbc.update("update owner_payout_destinations set status='PENDING_VERIFICATION',verified_at=null,verified_by=null where id=?",d);
        jdbc.update("update owner_payouts set payout_destination_id=? where id=?",d,p);
        req(post("/api/admin/payouts/"+p+"/mark-paid"),admin,Map.of("externalTransferReference","QA unverified"),409);assertEquals("READY",payoutStatus(p));assertEquals(0,Collections.frequency(audit(b),"PAYOUT_MARKED_PAID"));
    }
    @Autowired OwnerPayoutRepository payoutRepository;
    @Test void readinessWorkersClaimDisjointBookingsInIndependentTransactions()throws Exception{
        UUID b=confirmed();jdbc.update("update owner_payouts set eligible_at=now()-interval '1 day' where booking_id=?",b);
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);var tx=new TransactionTemplate(transactions);
        try{
            var first=pool.submit(()->tx.execute(status->{var ids=payoutRepository.lockBookingIdsDueForReadiness(OffsetDateTime.now(clock),100);assertTrue(ids.contains(b));locked.countDown();try{assertTrue(release.await(15,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return ids;}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));var second=pool.submit(()->tx.execute(status->payoutRepository.lockBookingIdsDueForReadiness(OffsetDateTime.now(clock),100)));
            var secondIds=second.get(5,TimeUnit.SECONDS);release.countDown();var firstIds=first.get(10,TimeUnit.SECONDS);assertTrue(Collections.disjoint(firstIds,secondIds));assertFalse(secondIds.contains(b));
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void readinessWorkerHonorsExactEndUsingInjectedClock()throws Exception{
        UUID b=confirmed();destination();bookings.reconcileMollieFee(b,new BigDecimal("1.23"));
        UUID p=payout(b);Instant end=payouts.get(p).eligibleAt().toInstant();
        clock.now=end.minusSeconds(1);readiness.scan();assertEquals("CALCULATED",payoutStatus(p));
        clock.now=end;readiness.scan();assertEquals("READY",payoutStatus(p));assertEquals(end,payouts.get(p).readyAt().toInstant());
    }
}
