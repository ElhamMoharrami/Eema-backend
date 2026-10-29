package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.dto.request.CreatePolicyVersionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Production HTTP/service transactions against V11 in disposable PostgreSQL. */
class PolicyIntegrationTest extends AuthPostgresSupport {
    @org.springframework.boot.test.mock.mockito.MockBean MollieProviderClient mollie;
    @Autowired AdminPolicyService admin;
    @Autowired PolicyAcceptanceService policies;
    @Autowired UserRepository users;
    @SpyBean PolicyAcceptanceRepository acceptances;
    @SpyBean PolicyVersionRepository versions;
    final String run = "QA-"+UUID.randomUUID();
    JsonNode scout, owner, administrator;

    @BeforeEach void setupPolicies() throws Exception {
        for(var p:admin.list()) if(p.status()==PolicyStatus.PUBLISHED) admin.retire(p.id());
        scout=actor("SCOUT"); owner=actor("OWNER"); administrator=actor("SCOUT");
        jdbc.update("update users set role='ADMIN' where id=?",id(administrator));
    }
    JsonNode request(MockHttpServletRequestBuilder request,JsonNode actor,Object body,int expected) throws Exception {
        request.contextPath("/api");
        if(actor!=null)request.header("Authorization","Bearer "+access(actor));
        if(body!=null)request.contentType("application/json").content(json.writeValueAsBytes(body));
        var response=mvc.perform(request).andReturn().getResponse();
        assertEquals(expected,response.getStatus(),response.getContentAsString());
        return response.getContentAsByteArray().length==0?json.nullNode():json.readTree(response.getContentAsByteArray());
    }
    Map<String,Object> draft(PolicyType type) {
        return new HashMap<>(Map.of("policyType",type.name(),"version",run+"-"+type.ordinal(),"title","QA TEST CONTENT — NOT LEGAL TERMS", "effectiveAt","2099-01-01T00:00:00Z","content","QA TEST CONTENT — NOT LEGAL TERMS\n <b>exact & plain</b> \n"));
    }
    UUID create(PolicyType type) throws Exception {return UUID.fromString(request(post("/api/admin/policies"),administrator,draft(type),201).path("id").asText());}
    UUID replacement(PolicyType type) {return admin.create(new CreatePolicyVersionRequest(type,"QA-"+UUID.randomUUID(),"QA TEST CONTENT — NOT LEGAL TERMS",OffsetDateTime.now().plusYears(2),"QA TEST CONTENT — NOT LEGAL TERMS\nreplacement")).id();}
    Set<UUID> fullSet() throws Exception {Set<UUID> ids=new LinkedHashSet<>();for(var t:PolicyType.values()){UUID p=create(t);admin.publish(p);ids.add(p);}return ids;}
    JsonNode accept(JsonNode actor,Collection<UUID> ids,int status) throws Exception {return request(post("/api/policies/accept"),actor,Map.of("policyVersionIds",ids),status);}
    User user(JsonNode a){return users.findById(id(a)).orElseThrow();}

    @Test void lifecycleAllFiveAndAuthorization() throws Exception {
        assertFalse(read("/policies/status",access(scout),200).path("allRequiredAccepted").asBoolean());
        for(var type:PolicyType.values()) {
            UUID id=create(type);
            for(JsonNode denied:Arrays.asList(scout,owner,null)) {
                int status=denied==null?401:403;
                request(post("/api/admin/policies"),denied,draft(type),status);
                request(get("/api/admin/policies"),denied,null,status);
                request(post("/api/admin/policies/"+id+"/publish"),denied,null,status);
                request(post("/api/admin/policies/"+id+"/retire"),denied,null,status);
            }
            assertEquals("DRAFT",jdbc.queryForObject("select status from policy_versions where id=?",String.class,id));
            read("/policies/"+type+"/current",access(scout),404);
            var published=request(post("/api/admin/policies/"+id+"/publish"),administrator,null,200);
            assertEquals(draft(type).get("content"),published.path("content").asText());
            assertEquals(PolicyAcceptanceService.sha256((String)draft(type).get("content")),published.path("contentHash").asText());
            assertFalse(published.path("publishedAt").isNull());
            for(var a:List.of(scout,owner,administrator)) read("/policies/"+type+"/current",access(a),200);
            UUID next=replacement(type);admin.publish(next);
            assertEquals("RETIRED",jdbc.queryForObject("select status from policy_versions where id=?",String.class,id));
            assertEquals(published.path("content").asText(),jdbc.queryForObject("select content from policy_versions where id=?",String.class,id));
            assertEquals(published.path("contentHash").asText(),jdbc.queryForObject("select content_hash from policy_versions where id=?",String.class,id));
            assertEquals(1,jdbc.queryForObject("select count(*) from policy_versions where policy_type=? and status='PUBLISHED'",Integer.class,type.name()));
            request(post("/api/admin/policies/"+next+"/retire"),administrator,null,200);
            read("/policies/"+type+"/current",access(owner),404);
            // Spring framework events may occur; domain notification events must not.
            assertEquals(0,events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count());
        }
        for(String path:List.of("/policies/current","/policies/status","/policies/PRIVACY_POLICY/current"))read(path,null,401);
        accept(null,List.of(UUID.randomUUID()),401);
    }

    @Test void immutableTransitionsAndRequestContract() throws Exception {
        UUID p=create(PolicyType.PRIVACY_POLICY);
        request(post("/api/admin/policies"),administrator,draft(PolicyType.PRIVACY_POLICY),409);
        request(post("/api/admin/policies/"+p+"/retire"),administrator,null,409);
        for(String phase:List.of("DRAFT","PUBLISHED","RETIRED")) {
            if(phase.equals("PUBLISHED"))admin.publish(p);
            if(phase.equals("RETIRED"))admin.retire(p);
            request(put("/api/admin/policies"),administrator,draft(PolicyType.PRIVACY_POLICY),405);
            request(patch("/api/admin/policies"),administrator,draft(PolicyType.PRIVACY_POLICY),405);
            request(put("/api/admin/policies/"+p),administrator,draft(PolicyType.PRIVACY_POLICY),404);
            request(patch("/api/admin/policies/"+p),administrator,draft(PolicyType.PRIVACY_POLICY),404);
            if(!phase.equals("DRAFT"))request(post("/api/admin/policies/"+p+"/publish"),administrator,null,409);
            assertEquals(draft(PolicyType.PRIVACY_POLICY).get("content"),jdbc.queryForObject("select content from policy_versions where id=?",String.class,p));
        }
        for(String field:List.of("version","title","content")) {
            var body=draft(PolicyType.TERMS_OF_SERVICE);body.put(field," ");request(post("/api/admin/policies"),administrator,body,400);
            body.put(field,"x".repeat(field.equals("version")?51:field.equals("title")?201:200001));request(post("/api/admin/policies"),administrator,body,400);
        }
        for(String field:List.of("policyType","effectiveAt")){var body=draft(PolicyType.TERMS_OF_SERVICE);body.put(field,"invalid");request(post("/api/admin/policies"),administrator,body,400);}
        var unknown=draft(PolicyType.TERMS_OF_SERVICE);unknown.put("extra","ignored by current contract");request(post("/api/admin/policies"),administrator,unknown,201);
    }

    @Test void exactAcceptanceSubsetsDuplicatesAndMissingTypes() throws Exception {
        var ids=fullSet();var first=ids.iterator().next();
        assertFalse(accept(scout,List.of(first,first),200).path("allRequiredAccepted").asBoolean());
        assertEquals(1,rows("policy_acceptances","user_id",id(scout)));
        for(var a:List.of(scout,owner,administrator)) {
            assertTrue(accept(a,ids,200).path("allRequiredAccepted").asBoolean());
            var before=jdbc.queryForList("select * from policy_acceptances where user_id=? order by id",id(a));
            accept(a,ids,200);assertEquals(before,jdbc.queryForList("select * from policy_acceptances where user_id=? order by id",id(a)));
        }
        accept(scout,List.of(),400);accept(scout,List.of(UUID.randomUUID()),400);
        accept(scout,Arrays.asList((UUID)null),400);
        var tooMany=new ArrayList<>(ids);tooMany.add(UUID.randomUUID());accept(scout,tooMany,400);
        UUID draft=replacement(PolicyType.TERMS_OF_SERVICE);accept(scout,List.of(draft),400);admin.publish(draft);
        accept(scout,List.of(first),400);assertFalse(policies.status(user(scout)).allRequiredAccepted());
        assertThrows(com.eema.backend.exception.ForbiddenException.class,()->policies.requireCurrentPolicies(user(scout)));
        accept(scout,List.of(draft),200);assertDoesNotThrow(()->policies.requireCurrentPolicies(user(scout)));
        admin.retire(draft);accept(scout,List.of(draft),400);assertFalse(policies.status(user(scout)).allRequiredAccepted());
    }

    @Test void replacementGatesCommitmentsAndPreservesBookingRightsAndSnapshots() throws Exception {
        Set<UUID> current=fullSet();accept(scout,current,200);accept(owner,current,200);
        jdbc.update("update users set email_verified=true where id in (?,?)",id(scout),id(owner));
        UUID venue=UUID.randomUUID();
        jdbc.update("insert into venues(id,owner_id,name,neighbourhood,size_m2,hourly_rate_eur,minimum_booking_minutes,status,permitted_activities,timezone,minimum_lead_time_minutes) values(?,?,'QA Policy Venue','Test',100,100,60,'ACTIVE',array['FILM']::production_activity[],'Europe/Amsterdam',60)",venue,id(owner));
        var date=java.time.LocalDate.now().plusDays(10);
        jdbc.update("insert into venue_operating_hours(venue_id,day_of_week,start_time,end_time) values(?,?,'08:00','20:00')",venue,date.getDayOfWeek().name());
        var body=Map.of("venueId",venue,"shootDate",date.toString(),"startTime","09:00","endTime","10:00","rawBrief","QA policy snapshot","scoutContact",Map.of("name","QA Scout","email",email(scout)));
        UUID booking=UUID.fromString(request(post("/api/bookings"),scout,body,201).path("id").asText());
        var before=jdbc.queryForList("select policy_type,policy_version_id from booking_policy_versions where booking_id=? order by policy_type",booking);
        assertEquals(4,before.size());assertTrue(before.stream().noneMatch(x->x.get("policy_type").equals("PRIVACY_POLICY")));
        assertTrue(before.stream().allMatch(x->current.contains(x.get("policy_version_id"))));
        var incident=request(post("/api/bookings/"+booking+"/incidents"),scout,Map.of("category","OTHER","description","QA historical incident"),201);
        UUID next=replacement(PolicyType.PRIVACY_POLICY);admin.publish(next);
        for(var a:List.of(scout,owner)){read("/bookings",access(a),200);read("/bookings/"+booking,access(a),200);read("/bookings/"+booking+"/incidents",access(a),200);}
        assertEquals(incident.path("id"),read("/bookings/"+booking+"/incidents",access(owner),200).get(0).path("id"));
        read("/bookings/"+booking+"/cancellation-preview",access(scout),200);
        read("/owner/payouts",access(owner),200);read("/owner/payout-destinations",access(owner),200);read("/venues",null,200);
        call("login",Map.of("email",email(scout),"password",QA_PASSWORD),200);
        assertPolicyDenied(post("/api/bookings"),scout,body);
        assertPolicyDenied(post("/api/bookings/"+booking+"/checkout"),scout,null);
        for(String action:List.of("accept","decline"))assertPolicyDenied(post("/api/bookings/"+booking+"/"+action),owner,null);
        assertPolicyDenied(post("/api/venues/"+venue+"/submit-for-review"),owner,null);
        assertPolicyDenied(post("/api/owner/payout-destinations"),owner,Map.of("accountHolderName","QA Owner","iban","NL91ABNA0417164300"));
        var inquiry=Map.of("venueId",venue,"requestedDate",date.toString(),"requestedStartTime","12:00","requestedEndTime","13:00","message","QA request");
        assertPolicyDenied(post("/api/availability-inquiries"),scout,inquiry);
        UUID inquiryId=UUID.randomUUID(); // policy gate precedes lookup; no owned-resource denial claimed here
        assertPolicyDenied(post("/api/availability-inquiries/"+inquiryId+"/accept"),scout,null);
        assertPolicyDenied(post("/api/availability-inquiries/"+inquiryId+"/decline"),owner,null);
        assertPolicyDenied(post("/api/availability-inquiries/"+inquiryId+"/offer"),owner,Map.of("offeredDate",date.toString(),"offeredStartTime","12:00","offeredEndTime","13:00"));
        assertEquals("REQUESTED",jdbc.queryForObject("select status::text from bookings where id=?",String.class,booking));
        request(post("/api/bookings/"+booking+"/cancel"),scout,null,200);
        var payout=read("/owner/payouts",access(owner),200).path("content").get(0);
        assertEquals(booking.toString(),payout.path("bookingId").asText());
        read("/owner/payouts/"+payout.path("id").asText(),access(owner),200);
        // Eligible paid historical fixture: no provider call is necessary to download a receipt.
        jdbc.update("update bookings set status='CONFIRMED',confirmed_at=now(),paid_at=now(),total_paid_eur=100,cancellation_policy_version=?,grace_period_ends_at=now()+interval '1 day',full_refund_until=now()+interval '3 days',partial_refund_until=now()+interval '8 days',partial_refund_percentage=50 where id=?",BookingCancellationPolicy.VERSION,booking);
        var receipt=mvc.perform(get("/api/bookings/"+booking+"/receipt").contextPath("/api").header("Authorization","Bearer "+access(scout))).andReturn().getResponse();
        assertEquals(200,receipt.getStatus());assertEquals("application/pdf",receipt.getContentType());assertTrue(receipt.getContentAsByteArray().length>100);
        jdbc.update("update bookings set mollie_payment_id=? where id=?","tr_policy_"+booking,booking);
        when(mollie.createRefund(anyString(),eq(booking),any(),any())).thenAnswer(inv->new com.eema.backend.dto.response.MollieRefundResponse("re_policy_qa","queued",inv.getArgument(0),new com.eema.backend.dto.response.MolliePaymentResponse.Amount(inv.getArgument(2).toString(),"EUR")));
        String previewToken=read("/bookings/"+booking+"/cancellation-preview",access(scout),200).path("previewToken").asText();
        request(post("/api/bookings/"+booking+"/cancel"),scout,Map.of("previewToken",previewToken),200);
        assertEquals("QUEUED",read("/bookings/"+booking,access(scout),200).path("refundStatus").asText());
        verify(mollie).createRefund(anyString(),eq(booking),any(),any());

        assertEquals(before,jdbc.queryForList("select policy_type,policy_version_id from booking_policy_versions where booking_id=? order by policy_type",booking));
        accept(scout,List.of(next),200);accept(owner,List.of(next),200);
        var newBody=new HashMap<String,Object>(body);newBody.put("startTime","11:00");newBody.put("endTime","12:00");
        UUID another=UUID.fromString(request(post("/api/bookings"),scout,newBody,201).path("id").asText());
        request(post("/api/bookings/"+another+"/decline"),owner,null,200);
        UUID termsReplacement=replacement(PolicyType.TERMS_OF_SERVICE);admin.publish(termsReplacement);admin.retire(termsReplacement);
        admin.retire(next);
        assertEquals(before,jdbc.queryForList("select policy_type,policy_version_id from booking_policy_versions where booking_id=? order by policy_type",booking));
    }
    void assertPolicyDenied(MockHttpServletRequestBuilder req,JsonNode a,Object body)throws Exception {
        assertTrue(request(req,a,body,403).path("message").asText().contains("Current required policies"));
    }

    @Test void lifecycleAndAcceptancePublishNoDomainNotificationEvents() throws Exception {
        var before=events.stream(Object.class).filter(e->e.getClass().getPackageName().equals("com.eema.backend.event")).toList();
        Set<UUID> ids=fullSet();accept(scout,ids,200);accept(scout,ids,200);
        UUID next=replacement(PolicyType.TERMS_OF_SERVICE);admin.publish(next);admin.retire(next);
        assertEquals(before,events.stream(Object.class).filter(e->e.getClass().getPackageName().equals("com.eema.backend.event")).toList());
    }

    @RepeatedTest(3) void concurrentServicePublicationHasOneCurrentAndSafeConflict() throws Exception {
        UUID previous=replacement(PolicyType.TERMS_OF_SERVICE);admin.publish(previous);
        UUID a=replacement(PolicyType.TERMS_OF_SERVICE),b=replacement(PolicyType.TERMS_OF_SERVICE);
        CyclicBarrier readCurrent=new CyclicBarrier(2);
        doAnswer(inv->{Object result=mockingDetails(versions).getMockCreationSettings().getDefaultAnswer().answer(inv);readCurrent.await(15,TimeUnit.SECONDS);return result;})
            .when(versions).findByPolicyTypeAndStatus(PolicyType.TERMS_OF_SERVICE,PolicyStatus.PUBLISHED);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->publicationStatus(a));var second=pool.submit(()->publicationStatus(b));
            var results=new ArrayList<>(List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS)));Collections.sort(results);
            assertEquals(List.of(200,409),results);
            assertEquals(1,jdbc.queryForObject("select count(*) from policy_versions where policy_type='TERMS_OF_SERVICE' and status='PUBLISHED'",Integer.class));
            assertEquals(1,jdbc.queryForObject("select count(*) from policy_versions where id in (?,?) and status='DRAFT'",Integer.class,a,b));
        } finally {pool.shutdownNow();reset(versions);}
    }
    int publicationStatus(UUID id)throws Exception {
        return mvc.perform(post("/api/admin/policies/"+id+"/publish").contextPath("/api").header("Authorization","Bearer "+access(administrator))).andReturn().getResponse().getStatus();
    }

    @RepeatedTest(3) void duplicateConcurrentAcceptanceCommitsBothRequestsWithoutPartialSet() throws Exception {
        Set<UUID> ids=fullSet();UUID first=ids.stream().min(UUID::compareTo).orElseThrow();CyclicBarrier checked=new CyclicBarrier(2);
        doAnswer(inv->{boolean exists=(boolean)mockingDetails(acceptances).getMockCreationSettings().getDefaultAnswer().answer(inv);if(inv.getArgument(1).equals(first)&&!exists)checked.await(15,TimeUnit.SECONDS);return exists;})
                .when(acceptances).existsByUserIdAndPolicyVersionId(id(scout),first);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<JsonNode> call=()->accept(scout,ids,200);
            Future<JsonNode> a=pool.submit(call),b=pool.submit(call);
            assertTrue(a.get(30,TimeUnit.SECONDS).path("allRequiredAccepted").asBoolean());
            assertTrue(b.get(30,TimeUnit.SECONDS).path("allRequiredAccepted").asBoolean());
            assertEquals(5,rows("policy_acceptances","user_id",id(scout)));
        } finally {pool.shutdownNow();reset(acceptances);}
    }

    @RepeatedTest(3) void publicationDuringAcceptanceKeepsHistoryButNextGateRejects() throws Exception {
        Set<UUID> ids=fullSet();UUID next=replacement(PolicyType.TERMS_OF_SERVICE);
        CountDownLatch fetched=new CountDownLatch(1),published=new CountDownLatch(1);AtomicBoolean paused=new AtomicBoolean();
        doAnswer(inv->{Object result=mockingDetails(versions).getMockCreationSettings().getDefaultAnswer().answer(inv);if(Thread.currentThread().getName().equals("policy-accept-race")&&paused.compareAndSet(false,true)){fetched.countDown();assertTrue(published.await(15,TimeUnit.SECONDS));}return result;})
                .when(versions).findByStatusOrderByPolicyTypeAsc(PolicyStatus.PUBLISHED);
        ExecutorService pool=Executors.newSingleThreadExecutor(r->new Thread(r,"policy-accept-race"));
        try {
            Future<JsonNode> acceptance=pool.submit(()->accept(scout,ids,200));assertTrue(fetched.await(15,TimeUnit.SECONDS));
            admin.publish(next);published.countDown();acceptance.get(30,TimeUnit.SECONDS);
            assertEquals(5,rows("policy_acceptances","user_id",id(scout)));
            assertFalse(policies.status(user(scout)).allRequiredAccepted());
            assertThrows(com.eema.backend.exception.ForbiddenException.class,()->policies.requireCurrentPolicies(user(scout)));
            accept(scout,List.of(next),200);policies.requireCurrentPolicies(user(scout));
            assertEquals(6,rows("policy_acceptances","user_id",id(scout)));
        } finally {published.countDown();pool.shutdownNow();reset(versions);}
    }
}
