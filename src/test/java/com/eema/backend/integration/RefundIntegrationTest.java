package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.event.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** REF shared subsystem contract over real PostgreSQL; Mollie acceptance/cache is simulated. */
@Import(RefundIntegrationTest.Notifications.class)
class RefundIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @org.springframework.boot.test.context.TestConfiguration static class Notifications {
        final List<UUID> completions=new CopyOnWriteArrayList<>();
        @org.springframework.transaction.event.TransactionalEventListener
        public void completed(CancellationRefundCompletedEvent event){completions.add(event.bookingId());}
    }
    @Autowired BookingRepository repository;
    @Autowired BookingEventRepository auditRepository;
    @Autowired RefundCoordinator coordinator;
    @Autowired RefundService refunds;
    @Autowired BookingCancellationCoordinator cancellations;
    @Autowired org.springframework.context.ApplicationEventPublisher publisher;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired Notifications notices;
    @SpyBean OwnerPayoutService payouts;
    UUID confirmed() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
        jdbc.update("update bookings set mollie_payment_id=? where id=?",payment(b),b);
        bookings.processPaidPayment(new MolliePaymentResponse(payment(b),"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
        clearInvocations(payouts);return b;
    }
    String payment(UUID b){return "tr_"+b.toString().replace("-","");}
    MollieRefundResponse response(UUID b,String status,String amount){return new MollieRefundResponse("re_"+b,status,payment(b),new MolliePaymentResponse.Amount(amount,"EUR"));}
    JsonNode request(UUID b,JsonNode actor,Object body,int status)throws Exception{return req(post("/api/bookings/"+b+"/refund"),actor,body,status);}
    List<Object> snapshot(UUID b){return List.of(row(b),jdbc.queryForList("select row_to_json(p)::text from owner_payouts p where booking_id=?",String.class,b),jdbc.queryForList("select row_to_json(l)::text from owner_financial_liabilities l where booking_id=?",String.class,b),audit(b));}
    RefundCoordinator.Claim claim(UUID b){return coordinator.claim(b,id(admin),"QA admin");}
    UUID ready(UUID b){
        if(jdbc.queryForObject("select count(*) from owner_payout_destinations where owner_id=? and status='VERIFIED'",Integer.class,id(owner))==0)
            jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",UUID.randomUUID(),id(owner),UUID.randomUUID().toString(),id(admin));
        // Real readiness calculation after an ended interval, not a directly manufactured READY row.
        jdbc.update("update bookings set shoot_date=? where id=?",LocalDate.now().minusDays(1),b);
        bookings.reconcileMollieFee(b,new BigDecimal("1.50"));
        UUID p=jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b);
        assertEquals("READY",jdbc.queryForObject("select status from owner_payouts where id=?",String.class,p));return p;
    }
    String payout(UUID id){return jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where id=?",String.class,id);}
    void complete(UUID b){refunds.reconcileForPayment(payment(b));}

    @ParameterizedTest @ValueSource(strings={"OWNER","SCOUT","ANONYMOUS"})
    void authorizationDenialHasNoSideEffects(String role)throws Exception{UUID b=confirmed();var before=snapshot(b);request(b,role.equals("OWNER")?owner:role.equals("SCOUT")?scout:null,null,role.equals("ANONYMOUS")?401:403);assertEquals(before,snapshot(b));verifyNoInteractions(mollie);verifyNoInteractions(payouts);}
    @ParameterizedTest @EnumSource(value=BookingStatus.class,mode=EnumSource.Mode.EXCLUDE,names="CONFIRMED")
    void unsupportedBookingStateHasNoSideEffects(BookingStatus status)throws Exception{UUID b=confirmed();jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);var before=snapshot(b);request(b,admin,null,409);assertEquals(before,snapshot(b));verifyNoInteractions(mollie);verifyNoInteractions(payouts);}
    @Test void fullRefundIgnoresCustomClientEconomicsAndMutableVenuePrice()throws Exception{
        UUID b=confirmed();UUID v=repository.findById(b).orElseThrow().getVenueId();jdbc.update("update venues set hourly_rate_eur=9999 where id=?",v);
        when(mollie.createRefund(eq(payment(b)),eq(b),any(),any())).thenAnswer(call->{assertEquals(call.getArgument(3),jdbc.queryForObject("select refund_idempotency_key from bookings where id=?",UUID.class,b));assertEquals("CREATING",repository.findById(b).orElseThrow().getRefundStatus().name());return response(b,"queued","125.50");});
        var result=request(b,admin,Map.of("amount",1,"refundPercentage",37,"currency","USD","refundAmountEur",999999),202);
        assertEquals("CONFIRMED",result.path("status").asText());assertEquals("QUEUED",result.path("refundStatus").asText());assertNull(repository.findById(b).orElseThrow().getRefundedAt());
        verify(mollie).createRefund(eq(payment(b)),eq(b),eq(new BigDecimal("125.50")),any());assertEquals(1,Collections.frequency(audit(b),"REFUND_REQUESTED"));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","CANCELLATION","LATE"})
    void completionPreservesRefundOrigin(String origin)throws Exception{
        UUID b=confirmed();String amount="125.50";RefundCoordinator.Claim claim;
        if(origin.equals("ADMIN"))claim=claim(b);
        else if(origin.equals("CANCELLATION")){
            jdbc.update("update bookings set grace_period_ends_at=?,full_refund_until=?,partial_refund_until=? where id=?",OffsetDateTime.now(clock).minusDays(2),OffsetDateTime.now(clock).minusDays(1),OffsetDateTime.now(clock).plusDays(1),b);
            var actor=users.findById(id(scout)).orElseThrow();String proof=cancellations.preview(b,actor,OffsetDateTime.now(clock)).previewToken();claim=cancellations.cancel(b,actor,null,proof).refundClaim();amount="62.75";
        }else{jdbc.update("update bookings set status='EXPIRED',late_payment_detected_at=now() where id=?",b);claim=coordinator.claimLatePayment(b).orElseThrow();}
        String expected=origin.equals("ADMIN")?"REFUNDED":origin.equals("CANCELLATION")?"CANCELLED":"EXPIRED";
        var result=response(b,"refunded",amount);if(origin.equals("CANCELLATION")) {
            when(mollie.createRefund(eq(payment(b)),eq(b),eq(new BigDecimal("62.75")),eq(claim.idempotencyKey()))).thenReturn(result);
            refunds.requestCancellationRefund(claim);
            verify(mollie).createRefund(payment(b),b,new BigDecimal("62.75"),claim.idempotencyKey());
        } else coordinator.recordProviderState(claim,result);var first=repository.findById(b).orElseThrow().getRefundedAt();clearInvocations(payouts);
        for(int n=0;n<3;n++){clock.now=clock.now.plusSeconds(10);coordinator.reconcile(payment(b),result);}
        assertEquals(expected,state(b));assertEquals(first,repository.findById(b).orElseThrow().getRefundedAt());assertEquals(1,Collections.frequency(audit(b),"PAYMENT_REFUNDED"));verifyNoInteractions(payouts);
        assertEquals(origin.equals("CANCELLATION")?1:0,Collections.frequency(notices.completions,b));
        if(origin.equals("CANCELLATION"))assertEquals(new BigDecimal("62.75"),claim.amount());
    }
    @ParameterizedTest @ValueSource(strings={"queued","pending","processing","refunded","failed","canceled"})
    void providerStatusAndFailedEconomics(String status)throws Exception{UUID b=confirmed();var before=repository.findById(b).orElseThrow().getOwnerPayoutEur();var claim=claim(b);coordinator.recordProviderState(claim,response(b,status,"125.50"));var after=repository.findById(b).orElseThrow();assertEquals(status.toUpperCase(),after.getRefundStatus().name());assertEquals(status.equals("refunded")?"REFUNDED":"CONFIRMED",state(b));if(!status.equals("refunded")){assertNull(after.getRefundedAt());assertEquals(before,after.getOwnerPayoutEur());assertEquals(0,Collections.frequency(audit(b),"PAYMENT_REFUNDED"));}if(status.equals("failed")||status.equals("canceled")){var snap=snapshot(b);request(b,admin,null,409);assertEquals(snap,snapshot(b));}}
    @ParameterizedTest @ValueSource(strings={"wrongPayment","missingPayment","wrongId","missingId","wrongAmount","fraction","wrongCurrency","missingCurrency","missingAmount","missingValue","unknownStatus","creating","null"})
    void malformedProviderFactsCannotComplete(String variant)throws Exception{
        UUID b=confirmed();var claim=claim(b);coordinator.recordProviderState(claim,response(b,"pending","125.50"));var before=snapshot(b);var r=response(b,"refunded","125.50");
        r=switch(variant){case "wrongPayment"->new MollieRefundResponse(r.id(),r.status(),"tr_other",r.amount());case "missingPayment"->new MollieRefundResponse(r.id(),r.status(),null,r.amount());case "wrongId"->new MollieRefundResponse("re_other",r.status(),r.paymentId(),r.amount());case "missingId"->new MollieRefundResponse(null,r.status(),r.paymentId(),r.amount());case "wrongAmount"->response(b,"refunded","5.00");case "fraction"->response(b,"refunded","125.501");case "wrongCurrency"->new MollieRefundResponse(r.id(),r.status(),r.paymentId(),new MolliePaymentResponse.Amount("125.50","USD"));case "missingCurrency"->new MollieRefundResponse(r.id(),r.status(),r.paymentId(),new MolliePaymentResponse.Amount("125.50",null));case "missingAmount"->new MollieRefundResponse(r.id(),r.status(),r.paymentId(),null);case "missingValue"->new MollieRefundResponse(r.id(),r.status(),r.paymentId(),new MolliePaymentResponse.Amount(null,"EUR"));case "unknownStatus"->response(b,"unknown","125.50");case "creating"->response(b,"creating","125.50");default->null;};
        var invalid=r;assertThrows(IllegalStateException.class,()->coordinator.reconcile(payment(b),invalid));assertEquals(before,snapshot(b));
    }
    @ParameterizedTest @ValueSource(ints={3599,3600,3601})
    void ambiguousCreationRestartAndExactWindow(int seconds)throws Exception{
        UUID b=confirmed();AtomicReference<UUID> accepted=new AtomicReference<>();
        when(mollie.createRefund(eq(payment(b)),eq(b),any(),any())).thenAnswer(call->{UUID key=call.getArgument(3);assertEquals(key,jdbc.queryForObject("select refund_idempotency_key from bookings where id=?",UUID.class,b));accepted.set(key);throw new org.springframework.web.client.ResourceAccessException("QA response lost after acceptance");});request(b,admin,null,400);
        clock.now=clock.now.plusSeconds(seconds);var tx=new TransactionTemplate(transactions);var fresh=new RefundCoordinator(repository,auditRepository,new BookingFinancials(),new BookingStateMachine(),publisher,payouts,clock);
        if(seconds<3600){var replay=tx.execute(status->fresh.claim(b,id(admin),"Admin"));assertEquals(accepted.get(),replay.idempotencyKey());assertEquals(new BigDecimal("125.50"),replay.amount());doReturn(response(b,"pending","125.50")).when(mollie).createRefund(eq(payment(b)),eq(b),any(),eq(accepted.get()));request(b,admin,null,202);verify(mollie,times(2)).createRefund(eq(payment(b)),eq(b),eq(new BigDecimal("125.50")),eq(accepted.get()));}
        else{request(b,admin,null,409);verify(mollie).createRefund(eq(payment(b)),eq(b),any(),any());assertEquals("CREATING",repository.findById(b).orElseThrow().getRefundStatus().name());}
        assertEquals(accepted.get(),repository.findById(b).orElseThrow().getRefundIdempotencyKey());assertEquals(1,Collections.frequency(audit(b),"REFUND_REQUESTED"));
    }
    @RepeatedTest(3) void repeatAmbiguousRestart() throws Exception { ambiguousCreationRestartAndExactWindow(30); }
    @RepeatedTest(3) void concurrentRequestsShareOneLogicalProviderRefund()throws Exception{
        UUID b=confirmed();var gate=new CyclicBarrier(2);var remote=new ConcurrentHashMap<UUID,MollieRefundResponse>();
        when(mollie.createRefund(eq(payment(b)),eq(b),any(),any())).thenAnswer(call->{UUID key=call.getArgument(3);assertEquals(key,jdbc.queryForObject("select refund_idempotency_key from bookings where id=?",UUID.class,b));gate.await(10,TimeUnit.SECONDS);return remote.computeIfAbsent(key,k->response(b,"pending","125.50"));});
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->request(b,admin,null,202));var c=pool.submit(()->request(b,admin,null,202));a.get(20,TimeUnit.SECONDS);c.get(20,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        assertEquals(1,remote.size());assertEquals(1,Collections.frequency(audit(b),"REFUND_REQUESTED"));assertEquals(new BigDecimal("125.50"),repository.findById(b).orElseThrow().getRefundAmountEur());
    }
    @Test void paidPayoutSurvivesRefundAndRepeatedRecalculation()throws Exception{
        UUID b=confirmed(),p=ready(b);payouts.markPaid(p,"QA real-ledger-reference",users.findById(id(admin)).orElseThrow());String historical=payout(p);
        when(mollie.createRefund(eq(payment(b)),eq(b),any(),any())).thenReturn(response(b,"pending","125.50"));request(b,admin,null,202);assertEquals(historical,payout(p));assertEquals(0,rows("owner_financial_liabilities","booking_id",b));
        when(mollie.getRefund(eq(payment(b)),anyString())).thenReturn(response(b,"refunded","125.50"));clearInvocations(payouts);complete(b);complete(b);complete(b);verify(payouts,times(1)).recalculateForBooking(b);
        payouts.recalculateForBooking(b);payouts.recalculateForBooking(b);assertEquals(historical,payout(p));assertEquals(1,rows("owner_financial_liabilities","booking_id",b));assertEquals(new BigDecimal("98.90"),jdbc.queryForObject("select amount_eur from owner_financial_liabilities where booking_id=? and type='OWNER_PAYOUT_RECOVERY'",BigDecimal.class,b));assertEquals(1,Collections.frequency(audit(b),"PAYMENT_REFUNDED"));assertEquals(0,Collections.frequency(notices.completions,b));
    }
    @RepeatedTest(3) void refundAndMarkPaidRaceBothSerializations()throws Exception{for(boolean payoutWins:List.of(false,true)){
        UUID b=confirmed(),p=ready(b);var administrator=users.findById(id(admin)).orElseThrow();var locked=new CountDownLatch(1);var attempted=new CountDownLatch(1);var release=new CountDownLatch(1);var tx=new TransactionTemplate(transactions);var pool=Executors.newFixedThreadPool(2);
        try{var first=pool.submit(()->tx.execute(status->{repository.findByIdForUpdate(b).orElseThrow();locked.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}if(payoutWins)payouts.markPaid(p,"QA bank transfer",administrator);else claim(b);return true;}));assertTrue(locked.await(10,TimeUnit.SECONDS));var second=pool.submit(()->{attempted.countDown();if(payoutWins)claim(b);else assertThrows(ConflictException.class,()->payouts.markPaid(p,"QA bank transfer",administrator));});assertTrue(attempted.await(10,TimeUnit.SECONDS));release.countDown();first.get(20,TimeUnit.SECONDS);second.get(20,TimeUnit.SECONDS);}finally{release.countDown();pool.shutdownNow();}
        var c=claim(b);coordinator.recordProviderState(c,response(b,"refunded","125.50"));assertEquals(payoutWins?"PAID":"CANCELLED",jdbc.queryForObject("select status from owner_payouts where id=?",String.class,p));assertEquals(payoutWins?1:0,rows("owner_financial_liabilities","booking_id",b));
    }}
    @Test void databaseProtectsRefundIdentityBoundsAndLiabilityUniqueness()throws Exception{
        UUID b=confirmed();var c=claim(b);for(String assignment:List.of("refund_amount_eur=-1","refund_amount_eur=126","refund_amount_eur=5","refund_idempotency_key=gen_random_uuid()","refund_idempotency_key=null","mollie_payment_id='tr_other'","refund_status='FAKE'"))assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set "+assignment+" where id=?",b));assertEquals(c.idempotencyKey(),repository.findById(b).orElseThrow().getRefundIdempotencyKey());
        coordinator.recordProviderState(c,response(b,"pending","125.50"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set mollie_refund_id='re_other' where id=?",b));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set mollie_refund_id=null where id=?",b));
        UUID otherBooking=confirmed();assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set refund_idempotency_key=? where id=?",c.idempotencyKey(),otherBooking));
        jdbc.update("insert into owner_financial_liabilities(owner_id,booking_id,type,amount_eur,status) values (?,?,'OWNER_PAYOUT_RECOVERY',1,'OPEN')",id(owner),b);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into owner_financial_liabilities(owner_id,booking_id,type,amount_eur,status) values (?,?,'OWNER_PAYOUT_RECOVERY',1,'OPEN')",id(owner),b));
    }

    @ParameterizedTest @ValueSource(ints={422,408,409,429,502})
    void rejectionAndAmbiguityKeepRecoverableIdentityAndSanitizeResponse(int status) throws Exception {
        UUID b=confirmed();var before=repository.findById(b).orElseThrow();
        byte[] sensitive="QA_PROVIDER_SECRET_BODY".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var code=org.springframework.http.HttpStatus.valueOf(status);
        var failure=status>=500 ? org.springframework.web.client.HttpServerErrorException.create(code,"",org.springframework.http.HttpHeaders.EMPTY,sensitive,null)
                : org.springframework.web.client.HttpClientErrorException.create(code,"",org.springframework.http.HttpHeaders.EMPTY,sensitive,null);
        when(mollie.createRefund(eq(payment(b)),eq(b),any(),any())).thenThrow(failure);
        var result=request(b,admin,null,400);assertFalse(result.toString().contains("QA_PROVIDER_SECRET_BODY"));
        var after=repository.findById(b).orElseThrow();assertNotNull(after.getRefundIdempotencyKey());
        assertEquals(status==422?RefundStatus.FAILED:RefundStatus.CREATING,after.getRefundStatus());
        assertEquals(before.getStatus(),after.getStatus());assertEquals(before.getOwnerPayoutEur(),after.getOwnerPayoutEur());
        assertNull(after.getRefundedAt());assertEquals(0,rows("owner_financial_liabilities","booking_id",b));
        assertEquals(1,Collections.frequency(audit(b),"REFUND_REQUESTED"));assertEquals(0,Collections.frequency(audit(b),"PAYMENT_REFUNDED"));
        assertEquals(0,Collections.frequency(notices.completions,b));
    }
    @Test void feeCanReconcileAfterAdministrativeRefundWithoutRestoringEntitlement()throws Exception{
        UUID b=confirmed();var c=claim(b);coordinator.recordProviderState(c,response(b,"refunded","125.50"));
        assertTrue(repository.findPendingFeeReconciliation(org.springframework.data.domain.PageRequest.of(0,1000)).stream().anyMatch(candidate->candidate.getId().equals(b)));
        bookings.reconcileMollieFee(b,new BigDecimal("1.50"));var row=repository.findById(b).orElseThrow();assertTrue(row.isMollieFeeReconciled());assertEquals(0,row.getOwnerPayoutEur().signum());assertEquals("REFUNDED",state(b));assertEquals("CANCELLED",jdbc.queryForObject("select status from owner_payouts where booking_id=?",String.class,b));
    }
}
