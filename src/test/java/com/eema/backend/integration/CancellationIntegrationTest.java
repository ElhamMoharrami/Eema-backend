package com.eema.backend.integration;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
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

/** Disposable PostgreSQL, real transactions and simulated provider. No claim of live Mollie parity. */
@Import(CancellationIntegrationTest.Faults.class)
class CancellationIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void freshActors(){owner=null;policies.clear();}
    static volatile UUID failBooking;
    static volatile BookingEventType failEvent;
    @org.springframework.boot.test.context.TestConfiguration
    static class Faults {
        @Bean static org.springframework.beans.factory.config.BeanPostProcessor cancellationFaults() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (!(bean instanceof BookingEventRepository)) return bean;
                    var proxy=new org.springframework.aop.framework.ProxyFactory(bean);
                    proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation->{
                        if (invocation.getMethod().getName().equals("save") && invocation.getArguments()[0] instanceof BookingEvent e
                                && Objects.equals(e.getBookingId(),failBooking) && e.getEventType()==failEvent)
                            throw new IllegalStateException("QA injected event persistence failure");
                        return invocation.proceed();
                    });
                    return proxy.getProxy();
                }
            };
        }
    }
    @Autowired BookingRepository repository;
    @Autowired BookingEventRepository bookingEvents;
    @Autowired RefundCoordinator refunds;
    @Autowired RefundService refundService;
    @Autowired BookingCancellationCoordinator cancellations;
    @Autowired OwnerPayoutService payouts;
    @Autowired OwnerFinancialLiabilityService liabilities;
    @Autowired org.springframework.context.ApplicationEventPublisher publisher;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @AfterEach void clearCancellationFaults(){failBooking=null;failEvent=null;}

    UUID confirmed() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
        jdbc.update("update bookings set mollie_payment_id=? where id=?",paymentId(b),b);
        bookings.processPaidPayment(new MolliePaymentResponse(paymentId(b),"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
        return b;
    }
    String paymentId(UUID b){return "tr_"+b.toString().replace("-","");}
    String token(UUID b,com.fasterxml.jackson.databind.JsonNode actor) throws Exception {
        return req(get("/api/bookings/"+b+"/cancellation-preview"),actor,null,200).path("previewToken").asText();
    }
    MollieRefundResponse response(UUID b,String status,String amount){return new MollieRefundResponse("re_"+b.toString().replace("-",""),status,paymentId(b),new MolliePaymentResponse.Amount(amount,"EUR"));}
    void cancel(UUID b,com.fasterxml.jackson.databind.JsonNode actor,String token,int status) throws Exception {
        req(post("/api/bookings/"+b+"/cancel"),actor,Map.of("reason","QA participant-visible reason","previewToken",token),status);
    }
    String economics(UUID b){return jdbc.queryForObject("select json_build_array(cancellation_policy_tier,cancellation_refund_percentage,cancellation_refund_amount_eur,cancellation_retained_amount_eur,cancelled_by_actor_type,cancellation_reason,cancelled_at,commission_eur)::text from bookings where id=?",String.class,b);}

    @ParameterizedTest @ValueSource(strings={"timeout","5xx","persistence"})
    void acceptedRefundResponseLossOrPersistenceFailureResumesFromDatabase(String failure) throws Exception {
        UUID b=confirmed();String proof=token(b,scout);AtomicReference<UUID> providerKey=new AtomicReference<>();AtomicInteger creations=new AtomicInteger();
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenAnswer(call->{
            UUID key=call.getArgument(3);
            // Independent JDBC connection observes a committed identity before provider acceptance.
            assertEquals(key,jdbc.queryForObject("select refund_idempotency_key from bookings where id=?",UUID.class,b));
            assertEquals("CANCELLED",state(b));
            assertEquals(new BigDecimal("125.50"),call.getArgument(2));
            if(providerKey.compareAndSet(null,key)) creations.incrementAndGet(); else assertEquals(providerKey.get(),key);
            if(failure.equals("persistence"))return response(b,"refunded","125.50");
            if(failure.equals("5xx"))throw org.springframework.web.client.HttpServerErrorException.create(org.springframework.http.HttpStatus.BAD_GATEWAY,"QA ambiguous",org.springframework.http.HttpHeaders.EMPTY,new byte[0],null);
            throw new org.springframework.web.client.ResourceAccessException("QA response lost after acceptance");
        });
        if(failure.equals("persistence")){failBooking=b;failEvent=BookingEventType.PAYMENT_REFUNDED;}
        cancel(b,scout,proof,failure.equals("persistence")?500:400);
        failBooking=null;failEvent=null;
        assertEquals("CREATING",detail(b).path("refundStatus").asText());String snapshot=economics(b);
        var fresh=new RefundCoordinator(repository,bookingEvents,new BookingFinancials(),new BookingStateMachine(),publisher,payouts,clock);
        var claim=fresh.resumableCancellationClaim(b).orElseThrow();assertEquals(providerKey.get(),claim.idempotencyKey());
        // Stateful fake replays its accepted object under the same key, not a second economic refund.
        doAnswer(call->{assertEquals(providerKey.get(),call.getArgument(3));return response(b,"refunded","125.50");}).when(mollie).createRefund(eq(paymentId(b)),eq(b),any(),any());
        var freshService=new RefundService(mock(CurrentUserService.class),refunds,mollie,bookings);
        assertTrue(freshService.resumeCancellationRefund(b));
        assertEquals(1,creations.get());assertEquals("REFUNDED",detail(b).path("refundStatus").asText());
        assertEquals(snapshot,economics(b));assertEquals(1,Collections.frequency(audit(b),"REFUND_REQUESTED"));
        assertEquals(1,Collections.frequency(audit(b),"PAYMENT_REFUNDED"));
        cancel(b,scout,proof,200);verify(mollie,times(2)).createRefund(eq(paymentId(b)),eq(b),any(),eq(providerKey.get()));
    }
    @Test void rollbackBeforeProviderLeavesNoCancellationOrClaim() throws Exception {
        UUID b=confirmed();String proof=token(b,scout);failBooking=b;failEvent=BookingEventType.REFUND_REQUESTED;
        cancel(b,scout,proof,500);assertEquals("CONFIRMED",state(b));
        assertNull(jdbc.queryForObject("select refund_idempotency_key from bookings where id=?",UUID.class,b));
        assertEquals(0,Collections.frequency(audit(b),"SCOUT_CANCELLED"));verifyNoInteractions(mollie);
    }
    @Test void staleTermsConflictAndReplayCannotChangeEconomics() throws Exception {
        UUID b=confirmed();jdbc.update("update bookings set grace_period_ends_at=?,full_refund_until=?,partial_refund_until=? where id=?",OffsetDateTime.now(clock).minusDays(1),OffsetDateTime.now(clock).plusSeconds(1),OffsetDateTime.now(clock).plusDays(1),b);
        String proof=token(b,scout);clock.now=clock.now.plusSeconds(1);
        var error=req(post("/api/bookings/"+b+"/cancel"),scout,Map.of("previewToken",proof),409);
        assertEquals("CANCELLATION_TERMS_CHANGED",error.path("code").asText());assertEquals("CONFIRMED",state(b));verifyNoInteractions(mollie);
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"queued","62.75"));
        String fresh=token(b,scout);cancel(b,scout,fresh,200);String snapshot=economics(b);
        cancel(b,scout,fresh,200);assertEquals(snapshot,economics(b));verify(mollie).createRefund(eq(paymentId(b)),eq(b),eq(new BigDecimal("62.75")),any());
    }
    @Test void concurrentReconciliationCompletesOnceWithImmutableCancellation() throws Exception {
        UUID b=confirmed();when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"pending","125.50"));cancel(b,scout,token(b,scout),200);String snapshot=economics(b);
        var gate=new CyclicBarrier(2);when(mollie.getRefund(eq(paymentId(b)),anyString())).thenAnswer(call->{gate.await(10,TimeUnit.SECONDS);return response(b,"refunded","125.50");});
        var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->refundService.reconcileForPayment(paymentId(b)));var c=pool.submit(()->refundService.reconcileForPayment(paymentId(b)));a.get(20,TimeUnit.SECONDS);c.get(20,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        assertEquals(1,Collections.frequency(audit(b),"PAYMENT_REFUNDED"));assertEquals(snapshot,economics(b));
        var time=detail(b).path("refundedAt");clock.now=clock.now.plusSeconds(60);
        refunds.reconcile(paymentId(b),response(b,"processing","125.50"));refunds.reconcile(paymentId(b),response(b,"refunded","125.50"));
        assertEquals(time,detail(b).path("refundedAt"));assertEquals("REFUNDED",detail(b).path("refundStatus").asText());
    }
    UUID ready(UUID b) {
        bookings.reconcileMollieFee(b,new BigDecimal("1.50"));
        UUID id=jdbc.queryForObject("select id from owner_payouts where booking_id=?",UUID.class,b);
        // Fixture represents a previously eligible payout with a verified historical destination.
        jdbc.update("update owner_payout_destinations set status='REPLACED',replaced_at=now() where owner_id=? and status='VERIFIED'",id(owner));
        UUID destination=UUID.randomUUID();
        jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at,verified_at,verified_by) values (?,?,'IBAN','EUR','QA Owner','qa-encrypted',?,'1234','VERIFIED',now(),now(),?)",destination,id(owner),destination.toString(),id(admin));
        jdbc.update("update owner_payouts set status='READY',ready_at=now(),payout_destination_id=? where id=?",destination,id);
        return id;
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void markPaidAndCancellationSerializeOnRealBookingLock(boolean payoutWins) throws Exception {
        UUID b=confirmed(),payout=ready(b);var actor=users.findById(id(owner)).orElseThrow();var administrator=users.findById(id(admin)).orElseThrow();String proof=token(b,owner);
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var attempted=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);var tx=new TransactionTemplate(transactions);
        try {
            var first=pool.submit(()->tx.execute(status->{repository.findByIdForUpdate(b).orElseThrow();locked.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}if(payoutWins)payouts.markPaid(payout,"QA-bank-transfer",administrator);else cancellations.cancel(b,actor,"QA visible reason",proof);return true;}));
            assertTrue(locked.await(10,TimeUnit.SECONDS));
            var second=pool.submit(()->{attempted.countDown();if(payoutWins)cancellations.cancel(b,actor,"QA visible reason",proof);else assertThrows(ConflictException.class,()->payouts.markPaid(payout,"QA-bank-transfer",administrator));});
            assertTrue(attempted.await(10,TimeUnit.SECONDS));release.countDown();first.get(20,TimeUnit.SECONDS);second.get(20,TimeUnit.SECONDS);
        } finally {release.countDown();pool.shutdownNow();}
        assertEquals("CANCELLED",state(b));
        assertEquals(payoutWins?"PAID":"CANCELLED",jdbc.queryForObject("select status from owner_payouts where id=?",String.class,payout));
        if(payoutWins){assertEquals(new BigDecimal("98.90"),jdbc.queryForObject("select payout_amount_eur from owner_payouts where id=?",BigDecimal.class,payout));assertEquals(new BigDecimal("98.90"),jdbc.queryForObject("select amount_eur from owner_financial_liabilities where booking_id=? and type='OWNER_PAYOUT_RECOVERY'",BigDecimal.class,b));}
    }
    @Test void feeReconciliationAndLiabilitySettlementLeaveCancellationAndRefundUnchanged() throws Exception {
        UUID b=confirmed();when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"refunded","125.50"));cancel(b,owner,token(b,owner),200);String snapshot=economics(b);
        assertEquals("REFUNDED",detail(b).path("refundStatus").asText());
        bookings.reconcileMollieFee(b,new BigDecimal("4.50"));assertEquals(snapshot,economics(b));
        UUID liability=jdbc.queryForObject("select id from owner_financial_liabilities where booking_id=?",UUID.class,b);
        var payout=jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where booking_id=?",String.class,b);
        liabilities.settle(liability,users.findById(id(admin)).orElseThrow());
        assertEquals("SETTLED",jdbc.queryForObject("select status from owner_financial_liabilities where id=?",String.class,liability));assertEquals(snapshot,economics(b));
        assertEquals(payout,jdbc.queryForObject("select row_to_json(p)::text from owner_payouts p where booking_id=?",String.class,b));
        assertThrows(ConflictException.class,()->bookings.reconcileMollieFee(b,new BigDecimal("9.00")));
        assertEquals(new BigDecimal("4.50"),jdbc.queryForObject("select amount_eur from owner_financial_liabilities where id=?",BigDecimal.class,liability));
    }
    @Autowired OperationalThresholds thresholds;
    @Autowired AdminOperationsService operations;
    @Autowired WorkflowObservability observability;
    @Autowired io.micrometer.core.instrument.MeterRegistry metrics;
    @Autowired FinancialDocumentService documents;

    @ParameterizedTest @ValueSource(ints={-1,0,1})
    void stuckQueueAndMetricShareExactThreshold(int seconds) throws Exception {
        UUID b=confirmed();when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"pending","125.50"));cancel(b,scout,token(b,scout),200);
        var cutoff=thresholds.refundCutoff(OffsetDateTime.now(clock));
        jdbc.update("update bookings set refund_requested_at=? where id=?",cutoff.minusSeconds(seconds),b);
        var active=List.of(RefundStatus.CREATING,RefundStatus.QUEUED,RefundStatus.PENDING,RefundStatus.PROCESSING);
        assertEquals(seconds>0,repository.findStuckRefunds(active,cutoff,org.springframework.data.domain.PageRequest.of(0,200)).stream().anyMatch(x->x.getId().equals(b)));
        assertEquals(seconds>0,operations.exceptions(org.springframework.data.domain.PageRequest.of(0,200)).stream()
                .anyMatch(x->b.equals(x.bookingId()) && x.type()==OperationalExceptionType.REFUND_STUCK));
        observability.scanStuckStates();
        assertEquals(repository.countByRefundStatusInAndRefundRequestedAtBefore(active,cutoff),metrics.get("eema.workflow.refund.stuck").gauge().value());
    }

    @Test void historicalDocumentsSurviveRefundAndRecovery() throws Exception {
        UUID b=confirmed(),payout=ready(b);var scoutUser=users.findById(id(scout)).orElseThrow();var ownerUser=users.findById(id(owner)).orElseThrow();
        byte[] receipt=documents.bookingReceipt(b,scoutUser).content();
        payouts.markPaid(payout,"QA historical transfer",users.findById(id(admin)).orElseThrow());
        byte[] statement=documents.payoutStatement(payout,ownerUser).content();
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"refunded","125.50"));cancel(b,owner,token(b,owner),200);
        assertArrayEquals(receipt,documents.bookingReceipt(b,scoutUser).content());
        assertArrayEquals(statement,documents.payoutStatement(payout,ownerUser).content());
        assertEquals(new BigDecimal("125.50"),repository.findById(b).orElseThrow().getTotalPaidEur());
        assertEquals(2,rows("owner_financial_liabilities","booking_id",b));
    }

    @Test void incidentModerationProfilesAndDeletionCannotRewriteHistory() throws Exception {
        UUID b=confirmed();UUID venue=repository.findById(b).orElseThrow().getVenueId();String original=row(b);
        var incident=req(post("/api/bookings/"+b+"/incidents"),scout,Map.of("category","OTHER","description","QA incident remains separate"),201);
        assertEquals(original,row(b));String proof=token(b,owner);
        jdbc.update("update venues set status='SUSPENDED',hourly_rate_eur=999 where id=?",venue);
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"pending","125.50"));cancel(b,owner,proof,200);String snapshot=economics(b);
        assertEquals("OPEN",jdbc.queryForObject("select status from incident_reports where id=?",String.class,uuid(incident)));
        String fullName=users.findById(id(owner)).orElseThrow().getFullName();
        try {
            jdbc.update("update users set full_name='QA changed profile',role='SCOUT' where id=?",id(owner));
            assertEquals(snapshot,economics(b));assertEquals("QA participant-visible reason",detail(b).path("cancellationReason").asText());
        } finally {jdbc.update("update users set full_name=?,role='OWNER' where id=?",fullName,id(owner));}
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("delete from venues where id=?",venue));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("delete from users where id=?",id(scout)));
        assertEquals(snapshot,economics(b));
    }

    @Test void paidPaymentDiscoveredDuringUnpaidWithdrawalRequiresFinancialPreview() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
        jdbc.update("update bookings set mollie_payment_id=? where id=?",paymentId(b),b);
        when(mollie.getPayment(paymentId(b))).thenReturn(new MolliePaymentResponse(paymentId(b),"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
        req(post("/api/bookings/"+b+"/cancel"),scout,null,409);assertEquals("CONFIRMED",state(b));
        verify(mollie,never()).createRefund(anyString(),any(),any(),any());
    }

    @Test void unexpectedPaymentAfterWithdrawalIsRefundedWithoutResurrectingBooking() throws Exception {
        UUID b=create(venue(owner,60),scout,"09:00","10:00");action(b,"accept",owner,200);
        jdbc.update("update bookings set mollie_payment_id=? where id=?",paymentId(b),b);
        when(mollie.getPayment(paymentId(b))).thenReturn(new MolliePaymentResponse(paymentId(b),"canceled",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
        req(post("/api/bookings/"+b+"/cancel"),scout,null,200);var cancelledAt=detail(b).path("cancelledAt");
        var outcome=bookings.processPaidPayment(new MolliePaymentResponse(paymentId(b),"paid",new MolliePaymentResponse.Amount("125.50","EUR"),null,Map.of("bookingId",b.toString())));
        assertEquals(BookingService.PaidPaymentResult.LATE_PAYMENT,outcome.result());
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"refunded","125.50"));
        refundService.requestLatePaymentRefund(b);
        assertEquals("CANCELLED",state(b));assertEquals("REFUNDED",detail(b).path("refundStatus").asText());
        assertEquals(cancelledAt,detail(b).path("cancelledAt"));
        assertEquals(0,jdbc.queryForObject("select count(*) from notification_outbox where aggregate_id=? and notification_type='LATE_PAYMENT_REFUND'",Integer.class,b));
        assertEquals(1,jdbc.queryForObject("select count(*) from notification_outbox where aggregate_id=? and notification_type='CANCELLATION_REFUND_COMPLETED'",Integer.class,b));
        assertEquals(0,jdbc.queryForObject("select payout_amount_eur from owner_payouts where booking_id=?",BigDecimal.class,b).signum());
        verify(mollie).createRefund(eq(paymentId(b)),eq(b),eq(new BigDecimal("125.50")),any());
    }

    @Test void rejectionKeepsCancelledBookingAndCannotMintNewLogicalRefund() throws Exception {
        UUID b=confirmed();String proof=token(b,scout);
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenThrow(org.springframework.web.client.HttpClientErrorException.create(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,"QA rejected",org.springframework.http.HttpHeaders.EMPTY,new byte[0],null));
        cancel(b,scout,proof,400);assertEquals("CANCELLED",state(b));assertEquals("FAILED",detail(b).path("refundStatus").asText());
        UUID key=repository.findById(b).orElseThrow().getRefundIdempotencyKey();
        assertThrows(ConflictException.class,()->refunds.claimCancellation(b,new BigDecimal("125.50"),id(scout),"QA",ActorType.SCOUT));
        cancel(b,scout,proof,200);assertEquals(key,repository.findById(b).orElseThrow().getRefundIdempotencyKey());
        verify(mollie).createRefund(eq(paymentId(b)),eq(b),any(),eq(key));
    }

    @ParameterizedTest @ValueSource(strings={"REQUESTED","open","canceled"})
    void canonicalUnpaidWithdrawalReleasesReservation(String providerState) throws Exception {
        UUID v=venue(owner,60), b=create(v,scout,"09:00","10:00");
        if(!providerState.equals("REQUESTED")) {
            action(b,"accept",owner,200);
            jdbc.update("update bookings set mollie_payment_id=? where id=?",paymentId(b),b);
            var amount=new MolliePaymentResponse.Amount("125.50","EUR");
            when(mollie.getPayment(paymentId(b))).thenReturn(new MolliePaymentResponse(paymentId(b),providerState,amount,null,null));
            if(providerState.equals("open")) when(mollie.cancelPayment(paymentId(b))).thenReturn(new MolliePaymentResponse(paymentId(b),"canceled",amount,null,null));
        }
        req(post("/api/bookings/"+b+"/cancel"),scout,null,200);
        assertEquals("CANCELLED",state(b));
        assertEquals("PRE_PAYMENT_WITHDRAWAL",detail(b).path("cancellationPolicyTier").asText());
        assertNull(repository.findById(b).orElseThrow().getRefundIdempotencyKey());
        verify(mollie,never()).createRefund(anyString(),any(),any(),any());
        if(providerState.equals("open"))verify(mollie).cancelPayment(paymentId(b));
        UUID replacement=create(v,second,"09:00","10:00");action(replacement,"accept",owner,200);
        assertEquals("AWAITING_PAYMENT",state(replacement));
    }

    @ParameterizedTest @ValueSource(strings={"OWNER","ADMIN"})
    void canonicalReasonBoundaryAndAuthoritativeFullRefund(String role) throws Exception {
        UUID b=confirmed();var actor=role.equals("OWNER")?owner:admin;String proof=token(b,actor);
        String before=row(b);int eventCount=audit(b).size();
        for(var body:List.of(Map.of("previewToken",proof),Map.of("reason"," ","previewToken",proof)))
            req(post("/api/bookings/"+b+"/cancel"),actor,body,409);
        req(post("/api/bookings/"+b+"/cancel"),actor,Map.of("reason","x".repeat(1001),"previewToken",proof),400);
        assertEquals(before,row(b));assertEquals(eventCount,audit(b).size());verifyNoInteractions(mollie);
        when(mollie.createRefund(eq(paymentId(b)),eq(b),any(),any())).thenReturn(response(b,"pending","125.50"));
        req(post("/api/bookings/"+b+"/cancel"),actor,Map.of("reason","x".repeat(1000),"previewToken",proof,
            "refundAmountEur",0,"refundPercentage",0),200);
        var stored=repository.findById(b).orElseThrow();
        assertEquals(new BigDecimal("125.50"),stored.getCancellationRefundAmountEur());
        assertEquals(new BigDecimal("0.00"),stored.getOwnerPayoutEur());
        assertEquals(role.equals("OWNER")?ProviderCostResponsibility.OWNER:ProviderCostResponsibility.EEMA,stored.getProviderCostResponsibility());
        assertEquals(1000,stored.getCancellationReason().length());
        verify(mollie).createRefund(eq(paymentId(b)),eq(b),eq(new BigDecimal("125.50")),any());
    }

    @Test void canonicalDeniedActorsHaveNoSideEffectsAndZeroRefundIsDurable() throws Exception {
        UUID b=confirmed();String before=row(b);int count=audit(b).size();
        req(get("/api/bookings/"+b+"/cancellation-preview"),null,null,401);
        req(post("/api/bookings/"+b+"/cancel"),null,null,401);
        for(var actor:List.of(other,second)) {
            req(get("/api/bookings/"+b+"/cancellation-preview"),actor,null,403);
            req(post("/api/bookings/"+b+"/cancel"),actor,null,403);
        }
        assertEquals(before,row(b));assertEquals(count,audit(b).size());verifyNoInteractions(mollie);
        jdbc.update("update bookings set grace_period_ends_at=?,full_refund_until=?,partial_refund_until=? where id=?",
            OffsetDateTime.now(clock).minusDays(2),OffsetDateTime.now(clock).minusDays(2),OffsetDateTime.now(clock).minusSeconds(1),b);
        String proof=token(b,scout);cancel(b,scout,proof,200);String snapshot=economics(b);cancel(b,scout,proof,200);
        assertEquals(snapshot,economics(b));assertEquals("CANCELLED",state(b));
        var stored=repository.findById(b).orElseThrow();assertEquals(new BigDecimal("0.00"),stored.getCancellationRefundAmountEur());
        assertEquals(RefundStatus.NONE,stored.getRefundStatus());assertNull(stored.getRefundIdempotencyKey());
        assertEquals(1,Collections.frequency(audit(b),"SCOUT_CANCELLED"));verifyNoInteractions(mollie);
        assertFalse(req(get("/api/bookings/"+b+"/cancellation-preview"),scout,null,200).path("canCancel").asBoolean());
    }

}
