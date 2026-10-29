package com.eema.backend.integration;

import com.eema.backend.event.*;
import com.eema.backend.service.*;
import com.eema.backend.config.NotificationOutboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real domain transactions and PostgreSQL locks; transport only is stubbed. No live provider. */
@TestPropertySource(properties={"app.notification-outbox.initial-delay-ms=86400000"})
class OperationsNotificationIntegrationTest extends BookingCoreSupport {
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactions;
    @Autowired NotificationOutboxStore store;
    @Autowired NotificationOutboxWriter writer;
    @Autowired NotificationOutboxDispatcher dispatcher;
    @Autowired NotificationOutboxProperties properties;
    @Autowired io.micrometer.core.instrument.MeterRegistry meters;
    @Autowired MarketplaceTrustAuditService trustAudit;
    @SpyBean EmailService emailProvider;

    @BeforeEach void isolate(){
        jdbc.update("delete from notification_outbox");
        clearInvocations(emailProvider);
        doReturn(true).when(emailProvider).enabled();
        doNothing().when(emailProvider).deliver(any(),any(),any());
    }
    TransactionTemplate tx(){return new TransactionTemplate(transactions);}
    UUID pending()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","10:00");
        jdbc.update("delete from notification_outbox where aggregate_id=?",b);
        action(b,"accept",owner,200);return b;
    }
    String status(UUID b){return jdbc.queryForObject("select status from notification_outbox where aggregate_id=?",String.class,b);}
    void advance(long seconds){clock.now=clock.now.plusSeconds(seconds);}

    @Test void domainCommitPersistsPendingWithoutDispatcherAndDuplicatePublicationDoesNotDuplicate()throws Exception{
        UUID b=pending();assertEquals("AWAITING_PAYMENT",state(b));assertEquals("PENDING",status(b));
        tx().executeWithoutResult(s->publisher.publishEvent(new BookingLifecycleEvent(b,BookingLifecycleEvent.Type.REQUEST_ACCEPTED)));
        assertEquals(1,rows("notification_outbox","aggregate_id",b));verify(emailProvider,never()).deliver(any(),any(),any());
        dispatcher.dispatchBatch();assertEquals("DELIVERED",status(b));verify(emailProvider,times(1)).deliver(any(),any(),any());
    }
    @Test void rollbackAndEnqueueFailureLeaveNoDomainTransitionOrNotification()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","10:00");jdbc.update("delete from notification_outbox");
        tx().executeWithoutResult(s->{jdbc.update("update bookings set status='DECLINED' where id=?",b);
            publisher.publishEvent(new BookingLifecycleEvent(b,BookingLifecycleEvent.Type.REQUEST_DECLINED));s.setRollbackOnly();});
        assertEquals("REQUESTED",state(b));assertEquals(0,rows("notification_outbox","aggregate_id",b));
        assertThrows(RuntimeException.class,()->tx().executeWithoutResult(s->{
            jdbc.update("update bookings set status='DECLINED' where id=?",b);
            writer.record("BOOKING",b,"REQUEST_DECLINED","test-failure",()->emailProvider.send(null,"s","t","h"));
        }));
        assertEquals("REQUESTED",state(b));assertEquals(0,rows("notification_outbox","aggregate_id",b));
        verify(emailProvider,never()).deliver(any(),any(),any());
    }
    @Test void actualBeforeCommitInsertFailureRollsBackAcceptanceAndAudit()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","10:00");String before=row(b);var history=audit(b);
        jdbc.execute("alter table notification_outbox add constraint qa_reject_accepted_outbox check(notification_type<>'REQUEST_ACCEPTED')");
        try{
            var response=mvc.perform(post("/api/bookings/"+b+"/accept").contextPath("/api")
                .header("Authorization","Bearer "+access(owner))).andReturn().getResponse();
            assertTrue(response.getStatus()>=400);assertEquals(before,row(b));assertEquals(history,audit(b));
            assertEquals(0,jdbc.queryForObject("select count(*) from notification_outbox where aggregate_id=? and notification_type='REQUEST_ACCEPTED'",Integer.class,b));
            verify(emailProvider).send(any(),eq("Your booking request was accepted"),any(),any());
            verify(emailProvider,never()).deliver(any(),any(),any());
        }finally{jdbc.execute("alter table notification_outbox drop constraint qa_reject_accepted_outbox");}
    }
    @Test void transientProviderFailureCannotUndoDomainAndRetriesAtExactBackoffBoundary()throws Exception{
        UUID b=pending();doThrow(new EmailService.DeliveryFailure("PROVIDER_HTTP_503",false)).doNothing().when(emailProvider).deliver(any(),any(),any());
        dispatcher.dispatchBatch();assertEquals("AWAITING_PAYMENT",state(b));assertEquals("PENDING",status(b));
        advance(29);dispatcher.dispatchBatch();verify(emailProvider,times(1)).deliver(any(),any(),any());
        advance(1);dispatcher.dispatchBatch();assertEquals("DELIVERED",status(b));verify(emailProvider,times(2)).deliver(any(),any(),any());
    }
    @Test void permanentFailureAndExhaustionAreSafeOperationalExceptions()throws Exception{
        UUID b=pending();doThrow(new EmailService.DeliveryFailure("PROVIDER_HTTP_422",true)).when(emailProvider).deliver(any(),any(),any());
        dispatcher.dispatchBatch();assertEquals("FAILED",status(b));
        var queue=req(get("/api/admin/operations/exceptions").param("size","100"),admin,null,200);
        String rendered=queue.toString();assertTrue(rendered.contains("NOTIFICATION_DELIVERY_FAILED"));assertTrue(rendered.contains("PROVIDER_HTTP_422"));
        assertEquals(1d,meters.get("eema.notifications.outbox").tag("status","FAILED").gauge().value());
        for(var meter:meters.find("eema.notifications.outbox").meters())
            assertTrue(meter.getId().getTags().stream().allMatch(t->t.getKey().equals("status")));
        assertFalse(rendered.contains("recipient_email"));assertFalse(rendered.contains("text_body"));assertFalse(rendered.contains("@example.test"));
        UUID second=pending();doThrow(new EmailService.DeliveryFailure("PROVIDER_HTTP_503",false)).when(emailProvider).deliver(any(),any(),any());
        jdbc.update("update notification_outbox set attempt_count=? where aggregate_id=?",properties.maxAttempts()-1,second);
        dispatcher.dispatchBatch();assertEquals("FAILED",status(second));
    }
    @Test void expiredLeaseRecoversAtEqualityAndOldWorkerCannotComplete()throws Exception{
        UUID b=pending();var first=store.claim().orElseThrow();assertTrue(store.claim().isEmpty());
        advance(properties.leaseSeconds()-1);assertTrue(store.claim().isEmpty());advance(1);
        var replacement=store.claim().orElseThrow();assertEquals(first.id(),replacement.id());assertNotEquals(first.claimId(),replacement.claimId());
        assertFalse(store.delivered(first));assertFalse(store.failed(first,"STALE",true));assertTrue(store.delivered(replacement));assertEquals("DELIVERED",status(b));
    }
    @Test void twoWorkersCannotSendTheSameClaimSimultaneously()throws Exception{
        UUID b=pending();var reached=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        doAnswer(i->{assertFalse(TransactionSynchronizationManager.isActualTransactionActive());reached.countDown();assertTrue(release.await(15,TimeUnit.SECONDS));return null;})
            .when(emailProvider).deliver(any(),any(),any());
        try{
            var first=pool.submit(dispatcher::dispatchBatch);assertTrue(reached.await(15,TimeUnit.SECONDS));
            pool.submit(dispatcher::dispatchBatch).get(10,TimeUnit.SECONDS);verify(emailProvider,times(1)).deliver(any(),any(),any());
            release.countDown();first.get(10,TimeUnit.SECONDS);assertEquals("DELIVERED",status(b));
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void skipLockedMakesProgressWhileAnotherConnectionHoldsCandidate()throws Exception{
        UUID a=pending(),b=pending();var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        try{
            var first=pool.submit(()->tx().executeWithoutResult(s->{jdbc.queryForList("select id from notification_outbox where aggregate_id=? for update",a);locked.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}}));
            assertTrue(locked.await(10,TimeUnit.SECONDS));assertEquals(b,store.claim().orElseThrow().aggregateId());release.countDown();first.get(10,TimeUnit.SECONDS);
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void providerAcceptedButCompletionLostRetriesStableIdentityAndHistoricalContent()throws Exception{
        UUID b=pending();var first=store.claim().orElseThrow();emailProvider.deliver(first.id(),first.sender(),first.draft());
        jdbc.update("update venues set name='Changed after event' where id=(select venue_id from bookings where id=?)",b);
        jdbc.update("update bookings set status='EXPIRED' where id=?",b);
        advance(properties.leaseSeconds());var retry=store.claim().orElseThrow();assertEquals(first.id(),retry.id());assertEquals(first.draft(),retry.draft());
        assertFalse(retry.draft().text().contains("Changed after event"));emailProvider.deliver(retry.id(),retry.sender(),retry.draft());store.delivered(retry);
        assertEquals("DELIVERED",status(b));verify(emailProvider,times(2)).deliver(eq(first.id()),any(),eq(first.draft()));
    }
    @Test void leaseDeathAtAttemptLimitBecomesTerminalAndDisabledEmailDoesNotConsumeClaims()throws Exception{
        UUID b=pending();doReturn(false).when(emailProvider).enabled();dispatcher.dispatchBatch();assertEquals("PENDING",status(b));
        assertEquals(0,jdbc.queryForObject("select attempt_count from notification_outbox where aggregate_id=?",Integer.class,b));
        jdbc.update("update notification_outbox set attempt_count=? where aggregate_id=?",properties.maxAttempts()-1,b);
        store.claim().orElseThrow();advance(properties.leaseSeconds());assertTrue(store.claim().isEmpty());assertEquals("FAILED",status(b));
    }
    @Test void authOutboxContainsNoCredentialAndReconstructsOriginalValidToken()throws Exception{
        var user=actor("SCOUT");String raw=verifier(id(user));String row=jdbc.queryForObject("select row_to_json(n)::text from notification_outbox n where aggregate_id=?",String.class,id(user));
        assertFalse(row.contains(raw));assertFalse(row.contains(raw.substring(raw.indexOf('.')+1)));
        var draft=org.mockito.ArgumentCaptor.forClass(EmailService.Draft.class);dispatcher.dispatchBatch();verify(emailProvider).deliver(any(),any(),draft.capture());
        assertTrue(draft.getValue().text().contains(raw));call("verify-email",Map.of("token",raw),200);assertEquals("DELIVERED",status(id(user)));
    }
    @Test void supersededAndExpiredAuthTokensAreNeverSent()throws Exception{
        var user=actor("SCOUT");call("resend-verification",Map.of("email",email(user)),200);
        dispatcher.dispatchBatch();assertEquals(1,jdbc.queryForObject("select count(*) from notification_outbox where aggregate_id=? and status='CANCELLED'",Integer.class,id(user)));
        verify(emailProvider,times(1)).deliver(any(),any(),any());
        jdbc.update("delete from notification_outbox");call("forgot-password",Map.of("email",email(user)),200);
        advance(3600);jdbc.update("update password_reset_tokens set expires_at=? where user_id=?",OffsetDateTime.now(clock),id(user));dispatcher.dispatchBatch();assertEquals("FAILED",status(id(user)));
        verify(emailProvider,times(1)).deliver(any(),any(),any());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"inactive","recipient-changed","used"})
    void authEligibilityIsRecheckedBeforeProvider(String condition)throws Exception{
        var user=actor("SCOUT");
        switch(condition){
            case "inactive" -> jdbc.update("update users set active=false where id=?",id(user));
            case "recipient-changed" -> jdbc.update("update users set email=? where id=?","changed-"+UUID.randomUUID()+"@example.test",id(user));
            case "used" -> jdbc.update("update email_verification_tokens set used_at=created_at+interval '1 second' where user_id=?",id(user));
        }
        dispatcher.dispatchBatch();assertEquals("CANCELLED",status(id(user)));verify(emailProvider,never()).deliver(any(),any(),any());
    }
    @Test void rotatedDerivationKeyFailsSafelyWithoutSendingCredential()throws Exception{
        var user=actor("SCOUT");var worker=new NotificationOutboxDispatcher(store,emailProvider,new OneTimeTokenCodec(),properties,new SimpleMeterRegistry());
        worker.dispatchBatch();assertEquals("FAILED",status(id(user)));verify(emailProvider,never()).deliver(any(),any(),any());
        assertEquals("AUTH_DERIVATION_KEY_CHANGED",jdbc.queryForObject("select last_failure_category from notification_outbox where aggregate_id=?",String.class,id(user)));
    }
    @Test void existingV13SchemaUpgradesWithoutRewritingHistoryAndHasClaimConstraints()throws Exception{
        String db="outbox_upgrade";
        assertEquals(0,POSTGRES.execInContainer("createdb","-U",POSTGRES.getUsername(),db).getExitCode());
        String url=POSTGRES.getJdbcUrl().replace("/eema_it","/"+db);
        var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource(url,POSTGRES.getUsername(),POSTGRES.getPassword());
        var upgrade=new org.springframework.jdbc.core.JdbcTemplate(ds);
        org.flywaydb.core.Flyway.configure().dataSource(ds).locations("classpath:db/migration/common").target("13").load().migrate();
        UUID marker=UUID.randomUUID();
        upgrade.update("insert into users(id,email,password_hash,full_name,role) values(?,?,'synthetic-hash','QA migration marker','SCOUT')",marker,"migration@example.test");
        String before=upgrade.queryForObject("select row_to_json(u)::text from users u where id=?",String.class,marker);
        var migration=org.flywaydb.core.Flyway.configure().dataSource(ds).locations("classpath:db/migration/common").load().migrate();
        assertEquals(1,migration.migrationsExecuted);assertEquals(before,upgrade.queryForObject("select row_to_json(u)::text from users u where id=?",String.class,marker));
        assertEquals(0,upgrade.queryForObject("select count(*) from notification_outbox",Integer.class));
        assertEquals(3,upgrade.queryForObject("select count(*) from pg_indexes where tablename='notification_outbox' and indexname in ('notification_due_idx','notification_lease_idx','notification_failed_idx')",Integer.class));
        UUID b=pending();
        for(String invalid:List.of("status='INVALID'","status='PROCESSING'","status='DELIVERED'","attempt_count=-1","aggregate_type='AUTH'"))
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update notification_outbox set "+invalid+" where aggregate_id=?",b));
        assertEquals("PENDING",status(b));
    }

    @Test void trustAuditIdentityDeduplicatesReplayButAllowsAnotherLegitimateTransition(){
        UUID v=venue(owner,60);
        tx().executeWithoutResult(s->{
            var type=com.eema.backend.entity.enums.TrustEventType.VENUE_SUBMITTED;
            var first=trustAudit.append(com.eema.backend.entity.enums.TrustSubjectType.VENUE,v,type,id(owner),com.eema.backend.entity.enums.ActorType.OWNER,null);
            var event=new TrustNotificationEvent(type,v,null,first);publisher.publishEvent(event);publisher.publishEvent(event);
        });
        assertEquals(1,rows("notification_outbox","aggregate_id",v));
        tx().executeWithoutResult(s->{
            var type=com.eema.backend.entity.enums.TrustEventType.VENUE_SUBMITTED;
            var second=trustAudit.append(com.eema.backend.entity.enums.TrustSubjectType.VENUE,v,type,id(owner),com.eema.backend.entity.enums.ActorType.OWNER,null);
            publisher.publishEvent(new TrustNotificationEvent(type,v,null,second));
        });
        assertEquals(2,rows("notification_outbox","aggregate_id",v));
    }
    @Test void recipientSlotsStayDistinctEvenWhenContactAddressesMatch()throws Exception{
        var request=body(venue(owner,60),"09:00","10:00");
        request.put("scoutContact",Map.of("name","QA shared contact","email",email(owner),"phone","+31000000000"));
        UUID b=uuid(req(post("/api/bookings"),scout,request,201));
        assertEquals(1,jdbc.queryForObject("select count(distinct recipient_email) from notification_outbox where aggregate_id=?",Integer.class,b));
        assertEquals(2,rows("notification_outbox","aggregate_id",b));
        assertEquals(List.of(0,1),jdbc.queryForList("select recipient_slot from notification_outbox where aggregate_id=? order by recipient_slot",Integer.class,b));
    }
}
