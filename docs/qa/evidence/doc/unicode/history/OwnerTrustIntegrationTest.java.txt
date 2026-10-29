package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.event.TrustNotificationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Owner review HTTP/security/transactions on disposable PostgreSQL; no identity provider. */
@org.springframework.context.annotation.Import(OwnerTrustIntegrationTest.NotificationCapture.class)
class OwnerTrustIntegrationTest extends AuthPostgresSupport {
    @SpyBean OwnerVerificationRepository verifications;
    @SpyBean CurrentUserService current;
    @SpyBean TrustNotificationService notifications;
    @SpyBean EmailService mail;
    @org.springframework.boot.test.context.TestConfiguration
    static class NotificationCapture {
        final List<TrustNotificationEvent> published = new CopyOnWriteArrayList<>();
        @org.springframework.context.event.EventListener
        public void capture(TrustNotificationEvent event) { published.add(event); }
    }
    @Autowired NotificationCapture capture;
    JsonNode owner,admin,scout,other;
    @BeforeEach void actors() throws Exception {
        owner=actor("OWNER");other=actor("OWNER");scout=actor("SCOUT");admin=actor("SCOUT");
        jdbc.update("update users set email_verified=true where id=?",id(owner));
        jdbc.update("update users set role='ADMIN' where id=?",id(admin));
    }
    JsonNode request(MockHttpServletRequestBuilder req,JsonNode actor,Object body,int expected)throws Exception {
        req.contextPath("/api");if(actor!=null)req.header("Authorization","Bearer "+access(actor));
        if(body!=null)req.contentType("application/json").content(json.writeValueAsBytes(body));
        var r=mvc.perform(req).andReturn().getResponse();assertEquals(expected,r.getStatus(),r.getContentAsString());
        return json.readTree(r.getContentAsByteArray());
    }
    JsonNode submit(JsonNode a,Object body,int expected)throws Exception{return request(post("/api/owner/verification/submit"),a,body,expected);}
    JsonNode review(UUID id,String action,Object body,JsonNode actor,int status)throws Exception{return request(post("/api/admin/owner-verifications/"+id+"/"+action),actor,body,status);}
    UUID pending(JsonNode a)throws Exception{return UUID.fromString(submit(a,Map.of("note","QA fictional owner context"),200).path("id").asText());}
    Map<String,Object> approval(){return new HashMap<>(Map.of("verificationMethod","MANUAL_REVIEW"));}
    Map<String,Object> row(UUID id){return jdbc.queryForMap("select * from owner_verifications where id=?",id);}
    List<Map<String,Object>> audit(UUID id){return jdbc.queryForList("select event_type,actor_id,actor_type,subject_type,note,created_at from marketplace_trust_events where subject_id=? order by created_at,id",id);}
    long published(UUID id){return capture.published.stream().filter(e->e.subjectId().equals(id)).count();}
    Object real(Object mock,org.mockito.invocation.InvocationOnMock invocation)throws Throwable{return mockingDetails(mock).getMockCreationSettings().getDefaultAnswer().answer(invocation);}

    @Test void fullLifecyclePreservesHistoryAndOwnerSafeProjection()throws Exception {
        var initial=read("/owner/verification",access(owner),200);assertTrue(initial.path("id").isNull());assertEquals("UNVERIFIED",initial.path("status").asText());assertEquals(0,rows("owner_verifications","owner_id",id(owner)));
        UUID id=pending(owner);var pending=row(id);assertEquals("PENDING",pending.get("status").toString());assertNotNull(pending.get("submitted_at"));assertNull(pending.get("reviewed_at"));assertNull(pending.get("reviewed_by"));assertEquals(0L,((Number)pending.get("version")).longValue());
        submit(owner,Map.of("note","Ignored duplicate note"),200);assertEquals(pending,row(id));assertEquals(1,audit(id).size());assertEquals(1,published(id));
        review(id,"reject",Map.of("reason","  QA clarify venue relationship  "),admin,200);var rejected=row(id);assertEquals("REJECTED",rejected.get("status").toString());assertEquals("QA clarify venue relationship",rejected.get("review_note"));assertEquals(this.id(admin),rejected.get("reviewed_by"));assertNotNull(rejected.get("reviewed_at"));
        var dto=read("/owner/verification",access(owner),200);assertEquals("QA clarify venue relationship",dto.path("reviewNote").asText());assertEquals(Set.of("id","ownerId","status","submittedAt","reviewedAt","reviewNote","verificationMethod"),fieldNames(dto));
        var resubmitted=submit(owner,Map.of("note","QA revised context"),200);assertEquals(id.toString(),resubmitted.path("id").asText());var resub=row(id);assertEquals("QA revised context",resub.get("owner_note"));for(String f:List.of("review_note","verification_method","reviewed_at","reviewed_by"))assertNull(resub.get(f));assertTrue(((java.sql.Timestamp)resub.get("submitted_at")).after((java.sql.Timestamp)pending.get("submitted_at")));
        var body=approval();body.put("note","QA owner-visible approval note");review(id,"approve",body,admin,200);var verified=row(id);assertEquals("VERIFIED",verified.get("status").toString());assertEquals("MANUAL_REVIEW",verified.get("verification_method"));assertEquals(3L,((Number)verified.get("version")).longValue());
        submit(owner,Map.of(),409);assertEquals(verified,row(id));assertEquals(4,published(id));
        var history=audit(id);assertEquals(List.of("OWNER_VERIFICATION_SUBMITTED","OWNER_VERIFICATION_REJECTED","OWNER_VERIFICATION_SUBMITTED","OWNER_VERIFICATION_APPROVED"),history.stream().map(e->e.get("event_type")).toList());
        for(int i=0;i<4;i++){assertEquals(i%2==0?this.id(owner):this.id(admin),history.get(i).get("actor_id"));assertEquals(i%2==0?"OWNER":"ADMIN",history.get(i).get("actor_type"));assertEquals("OWNER_VERIFICATION",history.get(i).get("subject_type"));assertEquals(i==1?"QA clarify venue relationship":null,history.get(i).get("note"));}
        assertEquals(0,rows("policy_acceptances","user_id",this.id(owner)));assertTrue(read("/owner/verification",access(other),200).path("id").isNull());
    }
    Set<String> fieldNames(JsonNode node){Set<String>s=new HashSet<>();node.fieldNames().forEachRemaining(s::add);return s;}

    @Test void currentDbEmailRoleAndAdminSecurityAreAuthoritative()throws Exception {
        read("/owner/verification",access(other),200);submit(other,Map.of(),403);
        jdbc.update("update users set email_verified=true where id=?",id(other));UUID p=pending(other); // same previously unverified JWT
        jdbc.update("update users set email_verified=false where id=?",id(other));var before=row(p);submit(other,Map.of(),403);assertEquals(before,row(p));
        for(var a:Arrays.asList(scout,admin,null)){int status=a==null?401:403;read("/owner/verification",a==null?null:access(a),status);submit(a,Map.of(),status);}
        for(var a:Arrays.asList(owner,other,scout,null)){int status=a==null?401:403;request(get("/api/admin/owner-verifications"),a,null,status);request(get("/api/admin/owner-verifications/"+p),a,null,status);review(p,"approve",approval(),a,status);review(p,"reject",Map.of("reason","QA denial"),a,status);}
        assertEquals(before,row(p));assertEquals(1,audit(p).size());assertEquals(1,published(p));
        jdbc.update("update users set active=false where id=?",id(owner));submit(owner,Map.of(),401);read("/owner/verification",access(owner),401);
    }

    @Test void onlyPendingCanBeReviewedAndApprovalHasNoVenueOrBankSideEffects()throws Exception {
        UUID p=pending(owner);List<UUID> venueIds=new ArrayList<>();for(String status:List.of("DRAFT","PENDING")){UUID v=UUID.randomUUID();venueIds.add(v);jdbc.update("insert into venues(id,owner_id,name,neighbourhood,size_m2,hourly_rate_eur,minimum_booking_minutes,status,permitted_activities,timezone,minimum_lead_time_minutes) values(?,?,'QA Trust Venue','Test',100,100,60,?::venue_status,array['FILM']::production_activity[],'Europe/Amsterdam',60)",v,id(owner),status);}
        var venues=jdbc.queryForList("select row_to_json(v)::text from venues v where owner_id=? order by id",id(owner));
        review(p,"approve",approval(),admin,200);assertEquals(venues,jdbc.queryForList("select row_to_json(v)::text from venues v where owner_id=? order by id",id(owner)));assertEquals(0,rows("owner_payout_destinations","owner_id",id(owner)));assertEquals(0,rows("owner_payouts","owner_id",id(owner)));
        for(String status:List.of("VERIFIED","REJECTED","UNVERIFIED")){
            UUID target=p;
            if(status.equals("REJECTED")){jdbc.update("update users set email_verified=true where id=?",id(other));target=pending(other);review(target,"reject",Map.of("reason","QA reason"),admin,200);}
            if(status.equals("UNVERIFIED")){target=UUID.randomUUID();jdbc.update("insert into owner_verifications(id,owner_id) values(?,?)",target,id(scout));} // schema-valid persisted state only; no logical ID exists normally
            var before=row(target);int count=audit(target).size();long notices=published(target);
            review(target,"approve",approval(),admin,409);review(target,"reject",Map.of("reason","QA"),admin,409);assertEquals(before,row(target));assertEquals(count,audit(target).size());assertEquals(notices,published(target));
        }
        for(String action:List.of("approve","reject"))review(UUID.randomUUID(),action,action.equals("approve")?approval():Map.of("reason","QA"),admin,404);
    }

    @ParameterizedTest @EnumSource(VerificationMethod.class)
    void everyMethodIsStoredWithoutAdditionalIdentityEvidence(VerificationMethod method)throws Exception {
        UUID p=pending(owner);review(p,"approve",Map.of("verificationMethod",method.name()),admin,200);
        assertEquals(method.name(),row(p).get("verification_method"));assertEquals(method.name(),read("/owner/verification",access(owner),200).path("verificationMethod").asText());
    }
    @ParameterizedTest @ValueSource(ints={-1,0,1,3,1000,1001})
    void submissionNotesRespectOptionalAndLengthContract(int size)throws Exception {
        String note=size<0?null:size==3?"   ":"é".repeat(size);var body=new HashMap<String,Object>();body.put("note",note);var result=submit(owner,body,size>1000?400:200);
        if(size>1000){assertEquals(0,rows("owner_verifications","owner_id",id(owner)));return;}
        assertEquals(note==null||note.isBlank()?null:note,row(UUID.fromString(result.path("id").asText())).get("owner_note"));
    }
    @ParameterizedTest @ValueSource(ints={-1,0,3,1000,1001})
    void approvalNotesRespectOptionalAndLengthContract(int size)throws Exception {
        UUID p=pending(owner);var before=row(p);var body=approval();body.put("note",size<0?null:size==3?"   ":"é".repeat(size));review(p,"approve",body,admin,size>1000?400:200);
        if(size>1000){assertEquals(before,row(p));assertEquals(1,audit(p).size());assertEquals(1,published(p));}else assertEquals(size<=3?null:"é".repeat(size),row(p).get("review_note"));
    }
    @Test void invalidMethodsReasonsAndUnknownFieldsCannotAssignState()throws Exception {
        var body=new HashMap<String,Object>();body.put("status","VERIFIED");body.put("ownerId",id(other));UUID p=UUID.fromString(submit(owner,body,200).path("id").asText());var before=row(p);
        for(var method:Arrays.asList(null,"UNKNOWN")){var b=new HashMap<String,Object>();b.put("verificationMethod",method);review(p,"approve",b,admin,400);}review(p,"approve",Map.of(),admin,400);
        for(String reason:Arrays.asList(null,"","   ","é".repeat(1001))){var b=new HashMap<String,Object>();b.put("reason",reason);review(p,"reject",b,admin,400);}review(p,"reject",Map.of(),admin,400);
        assertEquals(before,row(p));assertEquals(1,audit(p).size());assertEquals(1,published(p));
        review(p,"reject",Map.of("reason","é".repeat(1000),"status","VERIFIED","reviewedAt","2000-01-01T00:00:00Z"),admin,200);assertEquals("REJECTED",row(p).get("status").toString());submit(owner,null,200);
        var approve=approval();approve.put("ownerId",id(other));approve.put("reviewedAt","2000-01-01T00:00:00Z");approve.put("status","REJECTED");review(p,"approve",approve,admin,200);assertEquals(id(owner),row(p).get("owner_id"));assertEquals("VERIFIED",row(p).get("status").toString());
    }
    @Test void queueFiltersProjectionAndDatabaseConstraints()throws Exception {
        UUID p=pending(owner);var queue=read("/admin/owner-verifications?ownerId="+id(owner)+"&status=PENDING&size=500",access(admin),200);assertEquals(100,queue.path("size").asInt());assertEquals(1,queue.path("totalElements").asInt());assertEquals(p.toString(),queue.path("content").get(0).path("id").asText());
        var dto=read("/admin/owner-verifications/"+p,access(admin),200);assertEquals(Set.of("id","ownerId","status","ownerNote","reviewNote","verificationMethod","submittedAt","reviewedAt","reviewedBy","createdAt","updatedAt"),fieldNames(dto));
        assertEquals(0,read("/admin/owner-verifications?ownerId="+id(owner)+"&status=VERIFIED",access(admin),200).path("totalElements").asInt());read("/admin/owner-verifications?status=INVALID",access(admin),400);read("/admin/owner-verifications/"+UUID.randomUUID(),access(admin),404);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into owner_verifications(owner_id) values(?)",id(owner)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into owner_verifications(owner_id) values(?)",UUID.randomUUID()));
        for(String sql:List.of("update owner_verifications set status='BAD' where id=?","update owner_verifications set verification_method='BAD' where id=?","update owner_verifications set status='VERIFIED' where id=?","update owner_verifications set reviewed_at=now(),reviewed_by='00000000-0000-0000-0000-000000000000',status='REJECTED' where id=?"))assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update(sql,p));
        assertEquals("PENDING",row(p).get("status").toString());assertTrue(jdbc.queryForObject("select count(*)>0 from pg_indexes where indexname='idx_owner_verification_status'",Boolean.class));
    }

    @RepeatedTest(3) void simultaneousFirstSubmissionIsOneCommittedLifecycle()throws Exception {
        CyclicBarrier entered=new CyclicBarrier(2);
        doAnswer(inv->{Object u=inv.callRealMethod();if(TransactionSynchronizationManager.isActualTransactionActive()&&((User)u).getId().equals(id(owner)))entered.await(15,TimeUnit.SECONDS);return u;}).when(current).getCurrentUser();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->submit(owner,Map.of("note","QA first"),200));var b=pool.submit(()->submit(owner,Map.of("note","QA second"),200));var one=a.get(30,TimeUnit.SECONDS);var two=b.get(30,TimeUnit.SECONDS);assertEquals(one.path("id"),two.path("id"));assertEquals("PENDING",two.path("status").asText());UUID p=UUID.fromString(one.path("id").asText());assertEquals(1,rows("owner_verifications","owner_id",id(owner)));assertEquals(1,audit(p).size());assertEquals(1,published(p));}
        finally{pool.shutdownNow();reset(current,verifications);}
    }
    @RepeatedTest(3) void competingAdminReviewsHaveOneWinner()throws Exception {
        for(String competing:List.of("approve","reject")){
            JsonNode fresh=actor("OWNER");jdbc.update("update users set email_verified=true where id=?",id(fresh));UUID p=pending(fresh);CyclicBarrier start=new CyclicBarrier(2);
            doAnswer(inv->{start.await(15,TimeUnit.SECONDS);return real(verifications,inv);}).when(verifications).findByIdForUpdate(p);
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try{var a=pool.submit(()->reviewStatus(p,"approve",approval()));var b=pool.submit(()->reviewStatus(p,competing,competing.equals("approve")?approval():Map.of("reason","QA rejected")));var statuses=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);var r=row(p);assertEquals(2,audit(p).size());assertEquals(2,published(p));assertEquals(1L,((Number)r.get("version")).longValue());assertNotNull(r.get("reviewed_at"));assertEquals(id(admin),r.get("reviewed_by"));if(r.get("status").toString().equals("VERIFIED")){assertEquals("MANUAL_REVIEW",r.get("verification_method"));assertNull(r.get("review_note"));}else{assertNull(r.get("verification_method"));assertEquals("QA rejected",r.get("review_note"));}}
            finally{pool.shutdownNow();reset(verifications);}
        }
    }
    int reviewStatus(UUID p,String action,Object body)throws Exception{return mvc.perform(post("/api/admin/owner-verifications/"+p+"/"+action).contextPath("/api").header("Authorization","Bearer "+access(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andReturn().getResponse().getStatus();}
    @RepeatedTest(3) void rejectionVersusResubmissionHasOnlyLegalOrderedOutcomes()throws Exception {
        UUID p=pending(owner);CyclicBarrier start=new CyclicBarrier(2);
        doAnswer(inv->{start.await(15,TimeUnit.SECONDS);return real(verifications,inv);}).when(verifications).findByIdForUpdate(p);
        doAnswer(inv->{start.await(15,TimeUnit.SECONDS);return real(verifications,inv);}).when(verifications).findByOwnerIdForUpdate(id(owner));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->review(p,"reject",Map.of("reason","QA reason"),admin,200));var b=pool.submit(()->submit(owner,Map.of("note","QA resubmit"),200));a.get(30,TimeUnit.SECONDS);b.get(30,TimeUnit.SECONDS);var r=row(p);if(r.get("status").toString().equals("PENDING")){assertNull(r.get("review_note"));assertNull(r.get("reviewed_at"));assertEquals(3,audit(p).size());}else{assertEquals("REJECTED",r.get("status").toString());assertEquals("QA reason",r.get("review_note"));assertEquals(2,audit(p).size());}}
        finally{pool.shutdownNow();reset(verifications);}
    }

    @Test void notificationRunsAfterCommitAndFailureCannotRollBackTrust()throws Exception {
        CountDownLatch delivered=new CountDownLatch(1);var seen=new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(inv->{TrustNotificationEvent e=inv.getArgument(0);if(e.type()==TrustEventType.OWNER_VERIFICATION_SUBMITTED){seen.set(jdbc.queryForObject("select count(*) from owner_verifications where id=?",Integer.class,e.subjectId())==1);try{return inv.callRealMethod();}finally{delivered.countDown();}}return inv.callRealMethod();}).when(notifications).send(any());
        UUID p=pending(owner);assertTrue(delivered.await(10,TimeUnit.SECONDS));assertTrue(seen.get());verify(mail,timeout(10000)).send(eq(email(owner)),eq("Owner verification submitted"),contains("/owner/verification"),anyString());assertFalse(app.resend().enabled());
        CountDownLatch failed=new CountDownLatch(1);doAnswer(inv->{failed.countDown();throw new IllegalStateException("QA delivery failure");}).when(notifications).send(argThat(e->e.type()==TrustEventType.OWNER_VERIFICATION_APPROVED));
        review(p,"approve",approval(),admin,200);assertTrue(failed.await(10,TimeUnit.SECONDS));assertEquals("VERIFIED",row(p).get("status").toString());assertEquals(2,audit(p).size());
        reset(notifications);
    }
}
