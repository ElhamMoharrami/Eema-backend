package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.service.ai.ParsedBrief;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.event.TrustNotificationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** VEN HTTP, native PostgreSQL persistence and transaction races; only external payment transport is mocked. */
@org.springframework.context.annotation.Import(VenueDomainIntegrationTest.NotificationCapture.class)
class VenueDomainIntegrationTest extends AuthPostgresSupport {
    static final java.nio.file.Path MEDIA_ROOT=java.nio.file.Path.of(System.getProperty("java.io.tmpdir"),"eema-venue-it-"+UUID.randomUUID());
    @org.springframework.test.context.DynamicPropertySource static void mediaStorage(org.springframework.test.context.DynamicPropertyRegistry properties){properties.add("app.storage.upload-dir",MEDIA_ROOT::toString);}
    @AfterAll static void disposeMedia()throws Exception{if(java.nio.file.Files.exists(MEDIA_ROOT))try(var paths=java.nio.file.Files.walk(MEDIA_ROOT)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.delete(path);}}
    @Autowired VenueRepository venues;
    @MockBean MollieProviderClient mollie;
    @Autowired VenueFilterBuilder filter;
    @Autowired VenueMatchAvailabilityFilter availabilityFilter;
    @Autowired NotificationCapture capture;
    // Async notification reads must never race with mutable Mockito stubbing of this repository.
    record MediaFailure(UUID venueId,RuntimeException exception) {}
    static final java.util.concurrent.atomic.AtomicReference<MediaFailure> MEDIA_FAILURE=new java.util.concurrent.atomic.AtomicReference<>();
    record LockBarrier(UUID venueId, CyclicBarrier barrier, java.util.concurrent.atomic.AtomicInteger arrivals) {
        LockBarrier(UUID venueId,CyclicBarrier barrier) {this(venueId,barrier,new java.util.concurrent.atomic.AtomicInteger());}
    }
    static void disarmBarrier() {
        LockBarrier gate=LOCK_BARRIER.getAndSet(null);
        assertNotNull(gate);assertEquals(2,gate.arrivals().get(),"Both transactions must cross the real repository barrier");
    }
    static final java.util.concurrent.atomic.AtomicReference<LockBarrier> LOCK_BARRIER = new java.util.concurrent.atomic.AtomicReference<>();
    @org.springframework.boot.test.context.TestConfiguration
    static class NotificationCapture {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        MutableAvailabilityClock availabilityTestClock(){return new MutableAvailabilityClock();}
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor venueLockBarrierInterceptor() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean,String name) {
                    if (!(bean instanceof VenueRepository)) return bean;
                    var proxy = new org.springframework.aop.framework.ProxyFactory(bean);
                    proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
                        LockBarrier gate=LOCK_BARRIER.get();
                        if(gate!=null && invocation.getMethod().getName().equals("findByIdForUpdate")
                                && gate.venueId().equals(invocation.getArguments()[0])) {
                            gate.arrivals().incrementAndGet();gate.barrier().await(15,TimeUnit.SECONDS);
                        }
                        Object result=invocation.proceed();
                        MediaFailure failure=MEDIA_FAILURE.get();
                        if(failure!=null && invocation.getMethod().getName().equals("saveAndFlush")
                                && invocation.getArguments()[0] instanceof Venue saved && failure.venueId().equals(saved.getId()))
                            throw failure.exception(); // Real flush happened; force the surrounding transaction to roll back.
                        return result;
                    });
                    return proxy.getProxy();
                }
            };
        }
        final List<TrustNotificationEvent> events=new CopyOnWriteArrayList<>();
        final List<TrustNotificationEvent> committed=new CopyOnWriteArrayList<>();
        @org.springframework.transaction.event.TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
        public void committed(TrustNotificationEvent event){committed.add(event);}
        @org.springframework.context.event.EventListener public void capture(TrustNotificationEvent event){events.add(event);}
    }
    static JsonNode owner,other,scout,admin,secondAdmin;
    final LocalDate date=LocalDate.now().plusDays(20);
    static final List<UUID> policyIds=new ArrayList<>();
    static final List<String> BOOLEANS=List.of("naturalLight","blackoutPossible","loadingAccess","parkingAvailable","liftAvailable","wheelchairAccessible","bathroomsAvailable","soundRecordingSuitable");

    @BeforeEach void fixtures()throws Exception {
        if(owner!=null)return;
        owner=actor("OWNER");other=actor("OWNER");scout=actor("SCOUT");admin=actor("SCOUT");
        jdbc.update("update users set role='ADMIN' where id=?",id(admin));
        secondAdmin=actor("SCOUT");jdbc.update("update users set role='ADMIN' where id=?",id(secondAdmin));
        jdbc.update("update users set email_verified=true where id in (?,?,?)",id(owner),id(other),id(scout));
        for(PolicyType type:PolicyType.values()){
            var p=request(post("/api/admin/policies"),admin,Map.of("policyType",type,"version","QA-VEN-"+UUID.randomUUID(),"title","QA TEST CONTENT — NOT LEGAL TERMS","content","QA TEST CONTENT — NOT LEGAL TERMS","effectiveAt",OffsetDateTime.now().toString()),201);
            UUID pid=uuid(p);request(post("/api/admin/policies/"+pid+"/publish"),admin,null,200);policyIds.add(pid);
        }
        for(var a:List.of(owner,other,scout))acceptPolicies(a);
        UUID verification=uuid(request(post("/api/owner/verification/submit"),owner,Map.of("note","QA fictional owner"),200));
        request(post("/api/admin/owner-verifications/"+verification+"/approve"),admin,Map.of("verificationMethod","MANUAL_REVIEW"),200);
    }
    void acceptPolicies(JsonNode actor)throws Exception {request(post("/api/policies/accept"),actor,Map.of("policyVersionIds",policyIds),200);}
    JsonNode request(MockHttpServletRequestBuilder request,JsonNode actor,Object body,int expected)throws Exception {
        request.contextPath("/api");if(actor!=null)request.header("Authorization","Bearer "+access(actor));
        if(body!=null)request.contentType("application/json").content(json.writeValueAsBytes(body));
        var response=mvc.perform(request).andReturn().getResponse();
        assertEquals(expected,response.getStatus(),response.getContentAsString());
        return response.getContentAsByteArray().length==0?json.nullNode():json.readTree(response.getContentAsByteArray());
    }
    UUID uuid(JsonNode node){return UUID.fromString(node.path("id").asText());}
    Map<String,Object> minimal(){var b=new HashMap<String,Object>();b.put("name","QA Venue "+UUID.randomUUID());b.put("neighbourhood","QA-"+UUID.randomUUID());b.put("sizeM2",100);b.put("hourlyRateEur",100);b.put("minimumBookingMinutes",60);b.put("minimumLeadTimeMinutes",60);b.put("tags",List.of("qa-venue"));return b;}
    UUID draft()throws Exception{return uuid(request(post("/api/venues"),owner,minimal(),201));}
    Map<String,Object> body(UUID id)throws Exception{
        var dto=request(get("/api/venues/"+id),owner,null,200);Map<String,Object> b=new HashMap<>();
        for(var field:UpdateVenueRequest.class.getRecordComponents())b.put(field.getName(),json.convertValue(dto.get(field.getName()),Object.class));return b;
    }
    JsonNode update(UUID id,Map<String,Object>b,JsonNode actor,int status)throws Exception{return request(put("/api/venues/"+id),actor,b,status);}
    String photo(){return app.storage().fileUrlPrefix().replaceAll("/+$","")+"/"+UUID.randomUUID()+".png";}
    UUID ready()throws Exception{
        UUID id=draft();var b=body(id);b.put("maxCrewSize",20);b.put("permittedActivities",List.of("FILM","PHOTO"));b.put("productionRules","QA fictional production rules");
        for(String field:BOOLEANS)b.put(field,true);
        b.put("photoPaths",List.of(photo(),photo()));update(id,b,owner,200);return id;
    }
    UUID active()throws Exception{UUID id=ready();submit(id,owner,200);moderate(id,"approve",admin,Map.of(),200);return id;}
    JsonNode submit(UUID id,JsonNode a,int status)throws Exception{return request(post("/api/venues/"+id+"/submit-for-review"),a,null,status);}
    JsonNode moderate(UUID id,String action,JsonNode a,Object b,int status)throws Exception{return request(post("/api/admin/venues/"+id+"/"+action),a,b,status);}
    String row(UUID id){return jdbc.queryForObject("select row_to_json(v)::text from venues v where id=?",String.class,id);}
    String status(UUID id){return jdbc.queryForObject("select status::text from venues where id=?",String.class,id);}
    List<Map<String,Object>> audit(UUID id){return jdbc.queryForList("select event_type,actor_id,actor_type,note from marketplace_trust_events where subject_id=? order by created_at,id",id);}
    long notifications(UUID id){return capture.events.stream().filter(e->e.subjectId().equals(id)).count();}
    void unchanged(UUID id,String before,int events,long notices){assertEquals(before,row(id));assertEquals(events,audit(id).size());assertEquals(notices,notifications(id));}
    void weekly(UUID id)throws Exception{request(put("/api/venues/"+id+"/availability/weekly"),owner,Map.of("intervals",List.of(Map.of("dayOfWeek",date.getDayOfWeek().name(),"startTime","08:00","endTime","20:00"))),200);}
    Map<String,Object> bookingBody(UUID venue,int hour){return Map.of("venueId",venue,"shootDate",date.toString(),"startTime",String.format("%02d:00",hour),"endTime",String.format("%02d:00",hour+1),"rawBrief","QA venue lifecycle","scoutContact",Map.of("name","QA Scout","email",email(scout)));}
    UUID booking(UUID venue,int hour)throws Exception{return uuid(request(post("/api/bookings"),scout,bookingBody(venue,hour),201));}
    String bookingSnapshot(UUID id){return jdbc.queryForObject("select row_to_json(b)::text from bookings b where id=?",String.class,id);}
    Object policies(UUID id){return jdbc.queryForList("select policy_type,policy_version_id from booking_policy_versions where booking_id=? order by policy_type",id);}
    ParsedBrief brief(Map<String,Object> fields){return json.convertValue(fields,ParsedBrief.class);}
    boolean candidate(UUID id,Map<String,Object> requirements){
        var fields=new HashMap<>(requirements);fields.put("neighbourhood",jdbc.queryForObject("select neighbourhood from venues where id=?",String.class,id));
        return filter.findCandidateVenues(brief(fields)).stream().anyMatch(v->v.getId().equals(id));
    }

    @Test void suspendedVenueCannotAcceptRequestedButKeepsAcceptedAndConfirmedHistory()throws Exception{
        UUID v=active();weekly(v);UUID requested=booking(v,9),accepted=booking(v,11),confirmed=booking(v,13);
        request(post("/api/bookings/"+accepted+"/accept"),owner,null,200);
        request(post("/api/bookings/"+confirmed+"/accept"),owner,null,200);
        // Payment settlement belongs to PAY; seed only its resulting historical CONFIRMED state.
        jdbc.update("update bookings set status='CONFIRMED',confirmed_at=now(),paid_at=now(),total_paid_eur=venue_price_eur where id=?",confirmed);
        var requestedBefore=bookingSnapshot(requested);var acceptedBefore=bookingSnapshot(accepted);var confirmedBefore=bookingSnapshot(confirmed);
        moderate(v,"suspend",admin,Map.of("reason","QA safety review"),200);
        request(post("/api/bookings/"+requested+"/accept"),owner,null,409);
        assertEquals(requestedBefore,bookingSnapshot(requested));assertEquals(acceptedBefore,bookingSnapshot(accepted));assertEquals(confirmedBefore,bookingSnapshot(confirmed));
        request(post("/api/bookings"),scout,bookingBody(v,15),404);
        request(post("/api/availability-inquiries"),scout,Map.of("venueId",v,"requestedDate",date.toString(),"requestedStartTime","17:00","requestedEndTime","18:00","message","QA inquiry"),404);
        assertFalse(candidate(v,Map.of()));
        for(UUID b:List.of(requested,accepted,confirmed))for(var a:List.of(owner,scout))request(get("/api/bookings/"+b),a,null,200);
        when(mollie.createPayment(eq(accepted),any(),any())).thenReturn(new MolliePaymentResponse("tr_venue_qa","open",new MolliePaymentResponse.Amount("100.00","EUR"),new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/qa")),Map.of("bookingId",accepted.toString())));
        request(post("/api/bookings/"+accepted+"/checkout"),scout,null,201);
        assertEquals("AWAITING_PAYMENT",jdbc.queryForObject("select status::text from bookings where id=?",String.class,accepted));
        assertEquals(confirmedBefore,bookingSnapshot(confirmed));
    }
    @Test void numericallyIdenticalCeilingSaveDoesNotTriggerRereview()throws Exception{
        UUID v=ready();var b=body(v);b.put("ceilingHeightM",3.5);update(v,b,owner,200);submit(v,owner,200);moderate(v,"approve",admin,Map.of(),200);
        int count=audit(v).size();update(v,body(v),owner,200);assertEquals("ACTIVE",status(v));assertEquals(count,audit(v).size());
    }

    @Test void creationAuthorizationDefaultsAndServerOwnedFields()throws Exception {
        var b=minimal();b.put("ownerId",id(other));b.put("status","ACTIVE");b.put("timezone","America/New_York");b.put("photoPaths",List.of(photo()));b.put("reviewedAt","2000-01-01T00:00:00Z");
        UUID v=uuid(request(post("/api/venues"),other,b,201)); // owner review not required
        var d=request(get("/api/venues/"+v),other,null,200);assertEquals(id(other).toString(),d.path("owner").path("id").asText());assertEquals("DRAFT",d.path("status").asText());assertEquals("Europe/Amsterdam",d.path("timezone").asText());assertEquals(0,d.path("photoPaths").size());
        for(String f:BOOLEANS)assertTrue(d.path(f).isNull());
        assertEquals(0,rows("venue_operating_hours","venue_id",v));assertEquals(0,rows("venue_availability_overrides","venue_id",v));assertEquals(0,audit(v).size());
        for(var a:Arrays.asList(scout,admin,null))request(post("/api/venues"),a,minimal(),a==null?401:403);
        jdbc.update("update users set email_verified=false where id=?",id(other));
        try{request(post("/api/venues"),other,minimal(),403);request(get("/api/venues/my"),other,null,200);}
        finally{jdbc.update("update users set email_verified=true where id=?",id(other));}
        var fresh=actor("OWNER");jdbc.update("update users set email_verified=true where id=?",id(fresh));
        request(post("/api/venues"),fresh,minimal(),201);assertEquals(0,rows("policy_acceptances","user_id",id(fresh)));assertEquals(0,rows("owner_verifications","owner_id",id(fresh)));
    }
    @Test void fullUpdateClearsOptionalDataAndCannotAssignOwnershipOrModeration()throws Exception {
        UUID v=ready();var b=body(v);
        b.put("fullDayRateEur",500);b.put("ceilingHeightM",3.25);b.put("parkingSpaces",3);b.put("floorNumber",-1);b.put("powerDetails","QA power");b.put("description","QA details");b.put("clipUrl","https://example.test/clip");b.put("prohibitedActivities",List.of("MUSIC_VIDEO"));
        update(v,b,owner,200);var loaded=body(v);assertEquals("QA power",loaded.get("powerDetails"));assertEquals(List.of("MUSIC_VIDEO"),loaded.get("prohibitedActivities"));
        for(String f:List.of("fullDayRateEur","maxCrewSize","permittedActivities","prohibitedActivities","ceilingHeightM","parkingSpaces","floorNumber","productionRules","powerDetails","description","clipUrl"))loaded.put(f,null);
        for(String f:BOOLEANS)loaded.put(f,null);
        loaded.put("photoPaths",List.of());loaded.put("tags",List.of());loaded.put("ownerId",id(other));loaded.put("status","ACTIVE");loaded.put("reviewedAt","2000-01-01T00:00:00Z");
        var before=row(v);
        for(var a:Arrays.asList(other,scout,null)){update(v,loaded,a,a==null?401:403);assertEquals(before,row(v));}
        update(v,loaded,owner,200);var cleared=body(v);
        for(String f:List.of("fullDayRateEur","permittedActivities","prohibitedActivities","productionRules","description","clipUrl"))assertNull(cleared.get(f));
        for(String f:BOOLEANS)assertNull(cleared.get(f));
        assertEquals(List.of(),cleared.get("tags"));assertEquals(List.of(),cleared.get("photoPaths"));assertEquals("DRAFT",status(v));
        assertEquals(id(owner),jdbc.queryForObject("select owner_id from venues where id=?",UUID.class,v));assertNull(jdbc.queryForObject("select reviewed_at from venues where id=?",Object.class,v));
        update(v,cleared,admin,200);request(put("/api/venues/"+v),owner,Map.of("name","QA partial PUT"),400);
    }
    @Test void activityArraysPreserveUnknownEmptyAndEveryEnumAndRejectContradictions()throws Exception {
        UUID v=draft();var b=body(v);
        for(String field:List.of("permittedActivities","prohibitedActivities")){
            for(Object value:Arrays.asList(null,List.of(),Arrays.stream(ProductionActivity.values()).map(Enum::name).toList())){
                b.put(field,value);b.put(field.equals("permittedActivities")?"prohibitedActivities":"permittedActivities",null);
                update(v,b,owner,200);assertEquals(value,body(v).get(field));
                if(field.equals("permittedActivities")){jdbc.update("update venues set status='ACTIVE' where id=?",v);for(var activity:ProductionActivity.values())assertEquals(value!=null&&!((List<?>)value).isEmpty(),candidate(v,Map.of("productionActivity",activity.name())));jdbc.update("update venues set status='DRAFT' where id=?",v);}
                if(field.equals("permittedActivities")&&(value==null||((List<?>)value).isEmpty()))assertTrue(request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("readinessIssues").toString().contains("permittedActivities"));
            }
            b.put(field,List.of("FILM","FILM"));update(v,b,owner,400);
            b.put(field,List.of("ALIEN"));update(v,b,owner,400);b.put(field,null);
        }
        b.put("permittedActivities",List.of("FILM"));b.put("prohibitedActivities",List.of("FILM"));update(v,b,owner,400);
        assertEquals("production_activity[]",jdbc.queryForObject("select pg_typeof(permitted_activities)::text from venues where id=?",String.class,v));
    }
    static Stream<Arguments> parkingCases(){return Stream.of(Arguments.of(false,null,200),Arguments.of(false,0,200),Arguments.of(false,2,400),Arguments.of(true,null,200),Arguments.of(true,0,200),Arguments.of(true,2,200),Arguments.of(null,null,200),Arguments.of(null,2,200));}
    @ParameterizedTest @MethodSource("parkingCases") void parkingConsistencyAndHardRequirements(Boolean available,Integer spaces,int expected)throws Exception{
        UUID v=ready();var b=body(v);b.put("parkingAvailable",available);b.put("parkingSpaces",spaces);update(v,b,owner,expected);if(expected!=200)return;
        assertEquals(available,body(v).get("parkingAvailable"));assertEquals(spaces,body(v).get("parkingSpaces"));
        // Historical/admin data may have unknown optional facts; test SQL matching independently of moderation.
        jdbc.update("update venues set status='ACTIVE' where id=?",v);
        assertEquals(Boolean.TRUE.equals(available),candidate(v,Map.of("requiresParking",true)));
        assertEquals(Boolean.TRUE.equals(available)&&spaces!=null&&spaces>=1,candidate(v,Map.of("minimumParkingSpaces",1)));
        assertEquals(Boolean.TRUE.equals(available)&&spaces!=null,candidate(v,Map.of("minimumParkingSpaces",0)));
    }
    static Stream<Arguments> clips(){return Stream.of("https://example.test/clip","http://localhost/clip","http://127.0.0.1/clip").map(s->Arguments.of(s,200));}
    @ParameterizedTest @MethodSource("clips") void clipAllowsHttpLinksWithoutServerFetch(String clip,int code)throws Exception{UUID v=draft();var b=body(v);b.put("clipUrl",clip);update(v,b,owner,code);assertEquals(clip,body(v).get("clipUrl"));}
    @ParameterizedTest @ValueSource(strings={"javascript:alert(1)","data:text/html,test","file:///tmp/test","/relative","https:///missing-host","https://bad host","https://"})
    void unsafeOrMalformedClipsAreRejected(String clip)throws Exception{UUID v=draft();var b=body(v);b.put("clipUrl",clip);update(v,b,owner,400);b.put("clipUrl","https://example.test/"+"x".repeat(501));update(v,b,owner,400);}
    @ParameterizedTest @ValueSource(strings={"Europe/Amsterdam","America/New_York","UTC","+02:00"})
    void supportedZonesPersistAcrossBothRoutes(String zone)throws Exception{UUID v=draft();var b=body(v);b.put("timezone",zone);update(v,b,owner,200);assertEquals(zone,body(v).get("timezone"));request(put("/api/venues/"+v+"/availability/settings"),owner,Map.of("timezone",zone),200);assertEquals(zone,request(get("/api/venues/"+v+"/availability/settings"),owner,null,200).path("timezone").asText());}
    @ParameterizedTest @ValueSource(strings={"Invalid/Zone",""," "," Europe/Amsterdam ","europe/amsterdam"})
    void invalidZonesDoNotMutateEitherRoute(String zone)throws Exception{UUID v=draft();var before=row(v);var b=body(v);b.put("timezone",zone);update(v,b,owner,400);request(put("/api/venues/"+v+"/availability/settings"),owner,Map.of("timezone",zone),400);assertEquals(before,row(v));}
    static Stream<Arguments> criticalFields(){
        Map<String,Object> values=new LinkedHashMap<>();values.put("maxCrewSize",21);values.put("permittedActivities",List.of("FILM"));values.put("prohibitedActivities",List.of("VIDEO"));
        for(String f:BOOLEANS)values.put(f,false);
        values.put("ceilingHeightM",3.5);values.put("parkingSpaces",3);values.put("floorNumber",1);values.put("productionRules","QA updated rules");values.put("powerDetails","QA updated power");
        return values.entrySet().stream().map(e->Arguments.of(e.getKey(),e.getValue()));
    }
    @ParameterizedTest @MethodSource("criticalFields") void everyCriticalOwnerEditRequiresReviewAndPreservesBooking(String field,Object value)throws Exception{
        UUID v=active();weekly(v);UUID booking=booking(v,9);String historical=bookingSnapshot(booking);Object policy=policies(booking);int before=audit(v).size();
        var b=body(v);b.put(field,value);update(v,b,owner,200);assertEquals("PENDING",status(v));assertEquals(before+1,audit(v).size());assertEquals(audit(v).size(),notifications(v));
        assertFalse(request(get("/api/venues"),null,null,200).toString().contains(v.toString()));
        for(var a:Arrays.asList(scout,other,null))request(get("/api/venues/"+v),a,null,403);
        request(get("/api/venues/"+v),owner,null,200);assertFalse(candidate(v,Map.of()));assertEquals(historical,bookingSnapshot(booking));assertEquals(policy,policies(booking));
    }
    @ParameterizedTest @ValueSource(strings={"add","remove","reorder"})
    void photoListChangesRequireReview(String change)throws Exception{
        UUID v=active();weekly(v);UUID historicalBooking=booking(v,9);String historical=bookingSnapshot(historicalBooking);Object policyReferences=policies(historicalBooking);var b=body(v);var photos=new ArrayList<>((List<String>)b.get("photoPaths"));
        if(change.equals("add"))photos.add(photo());else if(change.equals("remove"))photos.remove(0);else Collections.reverse(photos);
        b.put("photoPaths",photos);update(v,b,owner,200);assertEquals("PENDING",status(v));assertEquals(photos,body(v).get("photoPaths"));assertEquals(3,audit(v).size());assertEquals(historical,bookingSnapshot(historicalBooking));assertEquals(policyReferences,policies(historicalBooking));
    }
    static Stream<Arguments> noncriticalFields(){
        return Stream.of(Arguments.of("name","QA renamed"),Arguments.of("neighbourhood","QA relocated"),Arguments.of("sizeM2",101),Arguments.of("hourlyRateEur",123.45),Arguments.of("minimumBookingMinutes",120),Arguments.of("minimumLeadTimeMinutes",120),Arguments.of("fullDayRateEur",500),Arguments.of("tags",List.of("qa-other","qa-venue")),Arguments.of("description","QA revised description"),Arguments.of("clipUrl","https://example.test/clip"),Arguments.of("timezone","America/New_York"));
    }
    @ParameterizedTest @MethodSource("noncriticalFields") void noncriticalEditsKeepApprovalAndBookingSnapshots(String field,Object value)throws Exception{
        UUID v=active();weekly(v);UUID booking=booking(v,9);String historical=bookingSnapshot(booking);Object policies=policies(booking);int count=audit(v).size();var b=body(v);b.put(field,value);
        update(v,b,owner,200);assertEquals("ACTIVE",status(v));assertEquals(count,audit(v).size());assertEquals(count,notifications(v));assertEquals(historical,bookingSnapshot(booking));assertEquals(policies,policies(booking));
        assertTrue(request(get("/api/venues"),null,null,200).toString().contains(v.toString()));
    }
    @Test void identicalAndAdminEditsAndEmptyAvailabilityCharacterizeApproval()throws Exception{
        UUID v=active();int count=audit(v).size();update(v,body(v),owner,200);assertEquals("ACTIVE",status(v));assertEquals(count,audit(v).size());
        var b=body(v);b.put("productionRules",null);update(v,b,admin,200);assertEquals("ACTIVE",status(v));assertEquals(count,audit(v).size());assertTrue(request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("readinessIssues").toString().contains("productionRules"));
        // Admin override is characterized, not turned into new marketplace policy.
        b.put("productionRules","QA restored");update(v,b,admin,200);weekly(v);
        request(put("/api/venues/"+v+"/availability/weekly"),owner,Map.of("intervals",List.of()),200);assertEquals("ACTIVE",status(v));assertEquals(count,audit(v).size());
        assertEquals(0,request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("readinessIssues").size());
        var candidates=filter.findCandidateVenues(brief(Map.of("neighbourhood",b.get("neighbourhood"))));assertTrue(candidates.stream().anyMatch(x->x.getId().equals(v)));
        assertTrue(availabilityFilter.retainAvailable(candidates,brief(Map.of("requestedDate",date.toString(),"requestedStartTime","09:00","requestedEndTime","10:00"))).isEmpty());
        for(var field:List.of("hourlyRateEur","minimumBookingMinutes","minimumLeadTimeMinutes","timezone")){var invalid=new HashMap<>(b);invalid.put(field,null);update(v,invalid,owner,400);}
    }
    static Stream<Arguments> triStates(){return BOOLEANS.stream().flatMap(f->Arrays.asList(null,false,true).stream().map(v->Arguments.of(f,v)));}
    @ParameterizedTest @MethodSource("triStates") void allEightBooleansPreserveThreeStatesThroughPostgresAndHardMatching(String field,Boolean value)throws Exception{
        UUID v=ready();var b=body(v);b.put(field,value);update(v,b,owner,200);assertEquals(value,body(v).get(field));
        String column=field.replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);assertEquals(value,jdbc.queryForObject("select "+column+" from venues where id=?",Boolean.class,v));
        jdbc.update("update venues set status='ACTIVE' where id=?",v); // matching fixture, not bypassing a production moderation request
        Map<String,String> required=Map.of("naturalLight","requiresNaturalLight","blackoutPossible","requiresBlackout","loadingAccess","requiresLoadingAccess","parkingAvailable","requiresParking","liftAvailable","requiresLift","wheelchairAccessible","requiresWheelchairAccessibility","bathroomsAvailable","requiresBathrooms","soundRecordingSuitable","requiresSoundRecordingSuitable");
        assertEquals(Boolean.TRUE.equals(value),candidate(v,Map.of(required.get(field),true)));assertTrue(candidate(v,Map.of()));
        Map<String,String> preferred=Map.of("naturalLight","prefersNaturalLight","blackoutPossible","prefersBlackout","loadingAccess","prefersLoadingAccess","parkingAvailable","prefersParking","liftAvailable","prefersLift");
        if(preferred.containsKey(field))assertTrue(candidate(v,Map.of(preferred.get(field),true)));
    }

    @Test void visibilityStatusActorMatrixAndExactTagNormalization()throws Exception{
        for(String state:List.of("DRAFT","PENDING","ACTIVE","SUSPENDED")){
            UUID v=draft();jdbc.update("update venues set status=?::venue_status,tags=array['qa-lower','QA-UPPER'] where id=?",state,v);
            for(var actor:Arrays.asList(owner,other,scout,admin,null)){
                int expected=state.equals("ACTIVE")||actor==owner||actor==admin?200:403;
                request(get("/api/venues/"+v),actor,null,expected);
            }
            assertEquals(state.equals("ACTIVE"),request(get("/api/venues"),null,null,200).toString().contains(v.toString()));
            assertEquals(state.equals("ACTIVE"),request(get("/api/venues").param("tag"," QA-LOWER "),null,null,200).toString().contains(v.toString()));
            assertFalse(request(get("/api/venues").param("tag","QA-UPPER"),null,null,200).toString().contains(v.toString())); // filter lowercases, storage does not
            assertFalse(request(get("/api/venues").param("tag","qa-absent"),null,null,200).toString().contains(v.toString()));
            assertEquals(state.equals("ACTIVE"),request(get("/api/venues").param("tag"," "),null,null,200).toString().contains(v.toString()));
        }
        var mine=request(get("/api/venues/my"),owner,null,200);for(var v:mine)assertEquals(id(owner).toString(),v.path("owner").path("id").asText());
        var theirs=request(get("/api/venues/my"),other,null,200);for(var v:theirs)assertEquals(id(other).toString(),v.path("owner").path("id").asText());
        for(var a:Arrays.asList(scout,admin,null))request(get("/api/venues/my"),a,null,403);
        request(get("/api/venues/"+UUID.randomUUID()),null,null,404);request(get("/api/venues/not-a-uuid"),null,null,400);
    }
    @Test void fullModerationLifecycleMetadataAuditNotificationsAndAdminQueue()throws Exception{
        UUID v=ready();var submitted=submit(v,owner,200);assertNotNull(submitted.get("submittedForReviewAt"));assertTrue(submitted.path("reviewedAt").isNull());
        moderate(v,"request-changes",admin,Map.of("reason","  QA clarify access  "),200);assertEquals("DRAFT",status(v));
        assertEquals("QA clarify access",request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("moderationReason").asText());
        var b=body(v);b.put("productionRules","QA revised rules");update(v,b,owner,200);submit(v,owner,200);
        var cleared=request(get("/api/venues/"+v+"/moderation"),owner,null,200);assertTrue(cleared.path("reviewedAt").isNull());assertTrue(cleared.path("moderationReason").isNull());
        moderate(v,"approve",admin,Map.of("note","QA reviewed"),200);
        moderate(v,"suspend",admin,Map.of("reason","QA suspension"),200);
        moderate(v,"reactivate",admin,Map.of("note","QA restored"),200);
        var history=audit(v);assertEquals(List.of("VENUE_SUBMITTED","VENUE_CHANGES_REQUESTED","VENUE_SUBMITTED","VENUE_APPROVED","VENUE_SUSPENDED","VENUE_REACTIVATED"),history.stream().map(e->e.get("event_type")).toList());assertEquals(6,notifications(v));
        for(int i=0;i<6;i++){boolean ownerEvent=i==0||i==2;assertEquals(id(ownerEvent?owner:admin),history.get(i).get("actor_id"));assertEquals(ownerEvent?"OWNER":"ADMIN",history.get(i).get("actor_type"));}
        assertEquals("QA clarify access",history.get(1).get("note"));assertEquals(id(admin),jdbc.queryForObject("select reviewed_by from venues where id=?",UUID.class,v));
        assertEquals("QA restored",jdbc.queryForObject("select moderation_reason from venues where id=?",String.class,v));
        var queue=request(get("/api/admin/venues").param("venueId",v.toString()).param("ownerId",id(owner).toString()).param("status","ACTIVE").param("size","500"),admin,null,200);
        assertEquals(100,queue.path("size").asInt());assertEquals(1,queue.path("totalElements").asInt());
        assertEquals(6,request(get("/api/admin/venues/"+v+"/events"),admin,null,200).size());
        assertFalse(app.resend().enabled());
    }
    @Test void replayedSubmissionHasNoSecondLifecycle() throws Exception {
        UUID venue = ready();
        submit(venue, owner, 200);
        String committed = row(venue);
        submit(venue, owner, 409);
        unchanged(venue, committed, 1, 1);
        assertEquals("PENDING", status(venue));
    }

    @Test void submissionPrerequisitesAndReviewRevalidationUseSupportedTransitions()throws Exception{
        UUID incomplete=draft();String before=row(incomplete);submit(incomplete,owner,409);unchanged(incomplete,before,0,0);
        UUID v=ready();before=row(v);submit(v,other,403);unchanged(v,before,0,0);
        jdbc.update("update users set email_verified=false where id=?",id(owner));
        try{submit(v,owner,403);unchanged(v,before,0,0);}finally{jdbc.update("update users set email_verified=true where id=?",id(owner));}
        UUID unreviewed=uuid(request(post("/api/venues"),other,minimal(),201));submit(unreviewed,other,403);
        var fresh=actor("OWNER");jdbc.update("update users set email_verified=true where id=?",id(fresh));UUID noPolicies=uuid(request(post("/api/venues"),fresh,minimal(),201));submit(noPolicies,fresh,403);assertEquals("DRAFT",status(noPolicies));
        submit(v,owner,200);var b=body(v);b.put("photoPaths",List.of());update(v,b,owner,200);assertEquals("PENDING",status(v));
        before=row(v);moderate(v,"approve",admin,Map.of(),409);unchanged(v,before,1,1);
        b.put("photoPaths",List.of(photo()));update(v,b,owner,200);moderate(v,"approve",admin,Map.of(),200);moderate(v,"suspend",admin,Map.of("reason","QA"),200);
        b=body(v);b.put("productionRules",null);update(v,b,owner,200);before=row(v);int count=audit(v).size();moderate(v,"reactivate",admin,Map.of(),409);unchanged(v,before,count,count);
        // Supported no-row owner verification, not invented revocation, exercises admin recheck.
        var readyBody=body(ready());readyBody.remove("timezone");readyBody.remove("photoPaths");UUID otherVenue=uuid(request(post("/api/venues"),other,readyBody,201));
        jdbc.update("update venues set status='PENDING' where id=?",otherVenue); // explicit legacy/queue fixture
        moderate(otherVenue,"approve",admin,Map.of(),409);
    }
    @Test void policiesAreRecheckedForSubmissionButNotAdminApproval()throws Exception{
        var fresh=actor("OWNER");jdbc.update("update users set email_verified=true where id=?",id(fresh));
        UUID verification=uuid(request(post("/api/owner/verification/submit"),fresh,Map.of(),200));request(post("/api/admin/owner-verifications/"+verification+"/approve"),admin,Map.of("verificationMethod","MANUAL_REVIEW"),200);
        UUID template=ready();var b=body(template);b.remove("timezone");b.remove("photoPaths");UUID v=uuid(request(post("/api/venues"),fresh,b,201));var full=new HashMap<>(b);full.put("timezone","Europe/Amsterdam");full.put("photoPaths",List.of(photo()));update(v,full,fresh,200);
        submit(v,fresh,403);acceptPolicies(fresh);submit(v,fresh,200);
        // Change authoritative email after submission: approval's contract rechecks readiness/owner review, not email.
        jdbc.update("update users set email_verified=false where id=?",id(fresh));moderate(v,"approve",admin,Map.of(),200);
    }
    @Test void wrongStateRoleAndInvalidReasonHaveNoModerationEffects()throws Exception{
        Map<String,String> source=Map.of("approve","PENDING","request-changes","PENDING","suspend","ACTIVE","reactivate","SUSPENDED");
        for(String state:List.of("DRAFT","PENDING","ACTIVE","SUSPENDED")){
            UUID v=ready();jdbc.update("update venues set status=?::venue_status where id=?",state,v);
            String before=row(v);
            for(String action:source.keySet()){
                Object body=action.equals("approve")||action.equals("reactivate")?Map.of():Map.of("reason","QA reason");
                if(!source.get(action).equals(state)){moderate(v,action,admin,body,409);unchanged(v,before,0,0);}
                for(var a:Arrays.asList(owner,other,scout,null)){moderate(v,action,a,body,a==null?401:403);unchanged(v,before,0,0);}
            }
            for(var a:Arrays.asList(owner,scout,null))for(String suffix:List.of("","/"+v,"/"+v+"/events"))request(get("/api/admin/venues"+suffix),a,null,a==null?401:403);
        }
        UUID v=ready();submit(v,owner,200);var before=row(v);
        for(Object b:List.of(Map.of(),Map.of("reason",""),Map.of("reason","   "),Map.of("reason","x".repeat(1001))))moderate(v,"request-changes",admin,b,400);
        moderate(v,"approve",admin,Map.of("note","x".repeat(1001)),400);unchanged(v,before,1,1);
        moderate(v,"request-changes",admin,Map.of("reason","é".repeat(1000)),200);assertEquals(1000,jdbc.queryForObject("select length(moderation_reason) from venues where id=?",Integer.class,v));
    }
    @Test void mediaUploadLimitsAuthorizationAndAvailabilityAreIndependent()throws Exception{
        UUID v=active();var before=row(v);byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAGUlEQVR4nGPUcNvHQApgIkn1qIZRDUNKAwDIDwFMvejdtwAAAABJRU5ErkJggg==");
        var file=new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/png",png);
        for(var a:Arrays.asList(other,scout,null))request(multipart("/api/venues/"+v+"/photos").file(file),a,null,a==null?401:403);
        assertEquals(before,row(v));
        var result=request(multipart("/api/venues/"+v+"/photos").file(file),owner,null,200);assertEquals("PENDING",result.path("status").asText());assertEquals(3,result.path("photoPaths").size());
        String stored=result.path("photoPaths").get(2).asText();
        var media=mvc.perform(get(stored.startsWith("/api/")?stored:"/api"+stored).contextPath("/api")).andReturn().getResponse();assertEquals(200,media.getStatus());assertArrayEquals(png,media.getContentAsByteArray());
        for(String ref:List.of("/etc/passwd","https://example.test/qa.png",app.storage().fileUrlPrefix()+"../qa.png")){
            var b=body(v);b.put("photoPaths",List.of(ref));update(v,b,owner,400);
        }
        var b=body(v);b.put("photoPaths",List.of(stored,stored));update(v,b,owner,400);b.put("photoPaths",IntStream.range(0,13).mapToObj(i->photo()).toList());update(v,b,owner,400);
        b.put("photoPaths",IntStream.range(0,12).mapToObj(i->photo()).toList());update(v,b,owner,200);before=row(v);request(multipart("/api/venues/"+v+"/photos").file(file),owner,null,400);assertEquals(before,row(v));
        request(multipart("/api/venues/"+v+"/photos").file(new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/png",new byte[0])),owner,null,400);
        for(var a:Arrays.asList(other,scout,null))request(put("/api/venues/"+v+"/availability/weekly"),a,Map.of("intervals",List.of()),a==null?401:403);
        int count=audit(v).size();weekly(v);var override=request(post("/api/venues/"+v+"/availability/overrides"),owner,Map.of("date",date.toString(),"type","BLOCKED","allDay",true),201);assertEquals(count,audit(v).size());
        request(delete("/api/venues/"+v+"/availability/overrides/"+uuid(override)),owner,null,204);assertEquals(count,audit(v).size());
    }
    int statusOnly(MockHttpServletRequestBuilder r,JsonNode actor,Object b)throws Exception{r.contextPath("/api").header("Authorization","Bearer "+access(actor));if(b!=null)r.contentType("application/json").content(json.writeValueAsBytes(b));return mvc.perform(r).andReturn().getResponse().getStatus();}
    @RepeatedTest(3) void concurrentAdminReviewsHaveOneAuthoritativeWinner()throws Exception{
        for(String contender:List.of("approve","request-changes")){
            UUID v=ready();submit(v,owner,200);CyclicBarrier barrier=new CyclicBarrier(2);
            LOCK_BARRIER.set(new LockBarrier(v,barrier));
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try{var a=pool.submit(()->statusOnly(post("/api/admin/venues/"+v+"/approve"),admin,Map.of("note","QA approval")));var b=pool.submit(()->statusOnly(post("/api/admin/venues/"+v+"/"+contender),secondAdmin,Map.of("reason","QA changes","note","QA competing")));
                var results=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);assertEquals(2,audit(v).size());assertEquals(2,notifications(v));assertTrue(Set.of("ACTIVE","DRAFT").contains(status(v)));}
            finally{pool.shutdownNow();disarmBarrier();}
        }
    }
    @RepeatedTest(3) void criticalEditVersusApprovalHasCoherentSerialization()throws Exception{
        UUID v=ready();submit(v,owner,200);var edited=body(v);edited.put("productionRules","QA concurrent rules");CyclicBarrier barrier=new CyclicBarrier(2);
        LOCK_BARRIER.set(new LockBarrier(v,barrier));ExecutorService pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->statusOnly(post("/api/admin/venues/"+v+"/approve"),admin,Map.of()));var b=pool.submit(()->statusOnly(put("/api/venues/"+v),owner,edited));
            assertEquals(200,a.get(30,TimeUnit.SECONDS));assertEquals(200,b.get(30,TimeUnit.SECONDS));assertEquals("QA concurrent rules",body(v).get("productionRules"));
            if(status(v).equals("ACTIVE")){assertEquals(2,audit(v).size());assertNotNull(jdbc.queryForObject("select reviewed_at from venues where id=?",Object.class,v));}
            else{assertEquals("PENDING",status(v));assertEquals(3,audit(v).size());assertNull(jdbc.queryForObject("select reviewed_at from venues where id=?",Object.class,v));}
        }finally{pool.shutdownNow();disarmBarrier();}
    }
    @RepeatedTest(3) void suspensionVersusRequestAcceptanceSerializesCommitmentBoundary()throws Exception{
        UUID v=active();weekly(v);UUID booking=booking(v,9);CyclicBarrier barrier=new CyclicBarrier(2);
        LOCK_BARRIER.set(new LockBarrier(v,barrier));ExecutorService pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->statusOnly(post("/api/admin/venues/"+v+"/suspend"),admin,Map.of("reason","QA concurrent suspension")));var b=pool.submit(()->statusOnly(post("/api/bookings/"+booking+"/accept"),owner,null));assertEquals(200,a.get(30,TimeUnit.SECONDS));int acceptance=b.get(30,TimeUnit.SECONDS);
            assertEquals("SUSPENDED",status(v));assertTrue(Set.of(200,409).contains(acceptance));assertEquals(acceptance==200?"AWAITING_PAYMENT":"REQUESTED",jdbc.queryForObject("select status::text from bookings where id=?",String.class,booking));assertEquals(3,audit(v).size());
        }finally{pool.shutdownNow();disarmBarrier();}
    }

    @Test void policyReplacementBlocksReadySubmissionUntilExactNewVersionAccepted()throws Exception{
        UUID v=ready();String before=row(v);
        UUID next=uuid(request(post("/api/admin/policies"),admin,Map.of("policyType","TERMS_OF_SERVICE","version","QA-VEN-"+UUID.randomUUID(),"title","QA TEST CONTENT — NOT LEGAL TERMS","content","QA TEST CONTENT — NOT LEGAL TERMS","effectiveAt","2099-01-01T00:00:00Z"),201));
        request(post("/api/admin/policies/"+next+"/publish"),admin,null,200);
        policyIds.set(0,next);
        try{submit(v,owner,403);unchanged(v,before,0,0);}
        finally{for(var a:List.of(owner,other,scout))acceptPolicies(a);}
        submit(v,owner,200);moderate(v,"approve",admin,Map.of(),200);
    }
    @Test void unverifiedEmailBlocksEveryOwnerMutationWithoutChangingExistingRows()throws Exception{
        UUID v=ready();var b=body(v);String before=row(v);jdbc.update("update users set email_verified=false where id=?",id(owner));
        try{
            update(v,b,owner,403);submit(v,owner,403);
            request(put("/api/venues/"+v+"/availability/settings"),owner,Map.of("timezone","UTC"),403);
            request(put("/api/venues/"+v+"/availability/weekly"),owner,Map.of("intervals",List.of()),403);
            request(post("/api/venues/"+v+"/availability/overrides"),owner,Map.of("date",date.toString(),"type","BLOCKED","allDay",true),403);
            request(multipart("/api/venues/"+v+"/photos").file(new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/png",new byte[]{1})),owner,null,403);
            request(get("/api/venues/"+v+"/availability/settings"),owner,null,200);
            unchanged(v,before,0,0);
        }finally{jdbc.update("update users set email_verified=true where id=?",id(owner));}
    }
    @Test void databaseConstraintsPreserveProductionAndOwnerInvariants()throws Exception{
        UUID v=ready();String before=row(v);
        for(String sql:List.of("update venues set owner_id='00000000-0000-0000-0000-000000000000' where id=?","update venues set permitted_activities=array['UNKNOWN']::production_activity[] where id=?","update venues set prohibited_activities=permitted_activities where id=?","update venues set parking_available=false,parking_spaces=1 where id=?","update venues set max_crew_size=0 where id=?"))
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update(sql,v));
        assertEquals(before,row(v));
    }
    @Test void activityOrderAndUnknownEmptyAreCriticalButTagOrderIsNot()throws Exception{
        for(String variant:List.of("activity-order","unknown-empty","tag-order")){
            UUID v=active();var b=body(v);int before=audit(v).size();
            if(variant.equals("activity-order"))b.put("permittedActivities",List.of("PHOTO","FILM"));
            else if(variant.equals("unknown-empty"))b.put("prohibitedActivities",List.of());
            else{b.put("tags",List.of("one","two"));update(v,b,owner,200);b.put("tags",List.of("two","one"));}
            update(v,b,owner,200);assertEquals(variant.equals("tag-order")?"ACTIVE":"PENDING",status(v));assertEquals(before+(variant.equals("tag-order")?0:1),audit(v).size());
        }
    }
    // READY: only API-reachable absences; NOT NULL pricing/timezone remain service-fixture cases.
    static Stream<Arguments> readinessAbsences() {
        return Stream.concat(Stream.of("maxCrewSize","permittedActivities","productionRules","naturalLight",
                "loadingAccess","bathroomsAvailable","soundRecordingSuitable").map(k->Arguments.of(k,null)),
                Stream.of(Arguments.of("permittedActivities",List.of()),Arguments.of("productionRules"," \t"),
                        Arguments.of("photoPaths",List.of())));
    }
    @ParameterizedTest @MethodSource("readinessAbsences")
    void readinessMissingFieldBlocksSubmitApproveAndReactivateUntilRestored(String key,Object missing)throws Exception {
        UUID v=ready();var baseline=body(v);
        for(String transition:List.of("submit","approve","reactivate")) {
            var b=body(v);b.put(key,missing);update(v,b,owner,200);
            var details=request(get("/api/venues/"+v+"/moderation"),owner,null,200);
            assertEquals(List.of(key),json.convertValue(details.path("readinessIssues"),List.class));
            var adminDetails=request(get("/api/admin/venues/"+v),admin,null,200);
            assertEquals(details.path("readinessIssues"),adminDetails.path("readinessIssues"));
            String before=row(v);int count=audit(v).size();long notices=notifications(v);
            if(transition.equals("submit"))submit(v,owner,409);else moderate(v,transition,admin,Map.of(),409);
            unchanged(v,before,count,notices);
            b.put(key,baseline.get(key));update(v,b,owner,200);
            assertTrue(request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("readinessIssues").isEmpty());
            if(transition.equals("submit"))submit(v,owner,200);else moderate(v,transition,admin,Map.of(),200);
            assertEquals(count+1,audit(v).size());assertEquals(notices+1,notifications(v));
            if(transition.equals("approve"))moderate(v,"suspend",admin,Map.of("reason","QA readiness reactivation"),200);
        }
    }

    static org.springframework.mock.web.MockMultipartFile mediaFile(String ext) {
        return new org.springframework.mock.web.MockMultipartFile("file","QA."+ext,
                ext.equals("jpg")||ext.equals("jpeg")?"image/jpeg":"image/"+ext,
                com.eema.backend.support.MediaFixtures.bytes("qa."+ext));
    }
    long mediaFiles()throws Exception {try(var files=java.nio.file.Files.list(MEDIA_ROOT)){return files.count();}}
    @ParameterizedTest @ValueSource(strings={"jpg","jpeg","png","webp"})
    void mediaStandaloneAndScopedFormatsUseRealFilesystemAndCorrectPublicHeaders(String ext)throws Exception {
        UUID v=draft();String before=row(v);long count=mediaFiles();
        String standalone=request(multipart("/api/upload/venue-photo").file(mediaFile(ext)),owner,null,200).path("url").asText();
        assertEquals(before,row(v));assertEquals(0,audit(v).size());assertEquals(count+1,mediaFiles());
        var attached=request(multipart("/api/venues/"+v+"/photos").file(mediaFile(ext)),owner,null,200);
        assertEquals(1,attached.path("photoPaths").size());assertEquals(count+2,mediaFiles());
        for(String ref:List.of(standalone,attached.path("photoPaths").get(0).asText())) {
            var response=mvc.perform(get(ref).contextPath("/api")).andReturn().getResponse();
            assertEquals(200,response.getStatus());assertEquals(mediaFile(ext).getContentType(),response.getContentType());
            assertArrayEquals(mediaFile(ext).getBytes(),response.getContentAsByteArray());
            assertEquals("nosniff",response.getHeader("X-Content-Type-Options"));
        }
        assertFalse(request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("readinessIssues").toString().contains("photoPaths"));
        var b=body(v);b.put("photoPaths",List.of());update(v,b,owner,200);
        assertTrue(request(get("/api/venues/"+v+"/moderation"),owner,null,200).path("readinessIssues").toString().contains("photoPaths"));
        assertEquals(count+2,mediaFiles());assertEquals(0,audit(v).size());assertEquals(0,notifications(v));
    }
    @Test void mediaAuthorizationInvalidContentAndMaxLimitNeverLeaveOrphans()throws Exception {
        UUID v=draft();String before=row(v);long count=mediaFiles();
        for(var a:Arrays.asList(other,scout,null))request(multipart("/api/venues/"+v+"/photos").file(mediaFile("jpg")),a,null,a==null?401:403);
        for(var a:Arrays.asList(admin,scout,null))request(multipart("/api/upload/venue-photo").file(mediaFile("jpg")),a,null,a==null?401:403);
        jdbc.update("update users set email_verified=false where id=?",id(owner));
        try{request(multipart("/api/upload/venue-photo").file(mediaFile("jpg")),owner,null,403);
            request(multipart("/api/venues/"+v+"/photos").file(mediaFile("jpg")),owner,null,403);}
        finally{jdbc.update("update users set email_verified=true where id=?",id(owner));}
        for(String route:List.of("/api/upload/venue-photo","/api/venues/"+v+"/photos")) {
            request(multipart(route),owner,null,400);
            for(var file:List.of(new org.springframework.mock.web.MockMultipartFile("file","evil.jpg","image/jpeg","not a jpeg".getBytes()),
                    new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/jpeg",mediaFile("jpg").getBytes()),
                    new org.springframework.mock.web.MockMultipartFile("file","qa.jpg","image/jpeg",new byte[0]),
                    new org.springframework.mock.web.MockMultipartFile("file","qa.jpg","image/jpeg",com.eema.backend.support.MediaFixtures.paddedJpeg(5242881))))
                request(multipart(route).file(file),owner,null,400);
        }
        unchanged(v,before,0,0);assertEquals(count,mediaFiles());
        for(int i=1;i<=12;i++)assertEquals(i,request(multipart("/api/venues/"+v+"/photos").file(mediaFile("png")),owner,null,200).path("photoPaths").size());
        before=row(v);request(multipart("/api/venues/"+v+"/photos").file(mediaFile("png")),owner,null,400);
        assertEquals(before,row(v));assertEquals(count+12,mediaFiles());
        var b=body(v);var refs=new ArrayList<>((List<String>)b.get("photoPaths"));
        b.put("photoPaths",List.of(refs.get(0),refs.get(0)));update(v,b,owner,400);assertEquals(before,row(v));
        Collections.reverse(refs);b.put("photoPaths",refs);update(v,b,owner,200);assertEquals(refs,body(v).get("photoPaths"));
        var list=request(get("/api/venues/my"),owner,null,200);var summary=java.util.stream.StreamSupport.stream(list.spliterator(),false).filter(n->uuid(n).equals(v)).findFirst().orElseThrow();
        assertEquals(refs.get(0),summary.path("photoPaths").get(0).asText());
        b.put("photoPaths",refs.subList(1,refs.size()));update(v,b,owner,200);assertEquals(count+12,mediaFiles());
        assertEquals(0,audit(v).size());assertEquals(0,notifications(v));
    }
    @Test void mediaAttachmentFailureAfterRealFlushRollsBackRowsAndDeletesNewObject()throws Exception {
        UUID v=active();String before=row(v);int auditCount=audit(v).size();long files=mediaFiles();
        long committed=capture.committed.stream().filter(e->e.subjectId().equals(v)).count();
        MEDIA_FAILURE.set(new MediaFailure(v,new IllegalStateException("QA forced persistence failure after flush")));
        try{request(multipart("/api/venues/"+v+"/photos").file(mediaFile("jpg")),owner,null,500);}
        finally{MEDIA_FAILURE.set(null);}
        assertEquals(before,row(v));assertEquals(auditCount,audit(v).size());assertEquals(files,mediaFiles());
        assertEquals(committed,capture.committed.stream().filter(e->e.subjectId().equals(v)).count());
    }
    @RepeatedTest(3) void mediaConcurrentUploadsAppendBothAndRespectLastSlot()throws Exception {
        for(boolean lastSlot:List.of(false,true)) {
            UUID v=draft();if(lastSlot){var b=body(v);b.put("photoPaths",IntStream.range(0,11).mapToObj(i->photo()).toList());update(v,b,owner,200);}
            long count=mediaFiles();LOCK_BARRIER.set(new LockBarrier(v,new CyclicBarrier(2)));var pool=Executors.newFixedThreadPool(2);
            try{var a=pool.submit(()->statusOnly(multipart("/api/venues/"+v+"/photos").file(mediaFile("jpg")),owner,null));
                var b=pool.submit(()->statusOnly(multipart("/api/venues/"+v+"/photos").file(mediaFile("png")),owner,null));
                var codes=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)));Collections.sort(codes);
                assertEquals(lastSlot?List.of(200,400):List.of(200,200),codes);
            }finally{pool.shutdownNow();disarmBarrier();}
            var refs=(List<?>)body(v).get("photoPaths");assertEquals(lastSlot?12:2,refs.size());assertEquals(refs.size(),new HashSet<>(refs).size());
            assertEquals(count+(lastSlot?1:2),mediaFiles());
        }
    }
    @RepeatedTest(3) void mediaConcurrentWholePutAndUploadHaveSerializedReplacementSemantics()throws Exception {
        UUID v=draft();request(multipart("/api/venues/"+v+"/photos").file(mediaFile("jpg")),owner,null,200);
        request(multipart("/api/venues/"+v+"/photos").file(mediaFile("png")),owner,null,200);
        var b=body(v);var original=(List<String>)b.get("photoPaths");var reordered=List.of(original.get(1),original.get(0));b.put("photoPaths",reordered);
        long count=mediaFiles();LOCK_BARRIER.set(new LockBarrier(v,new CyclicBarrier(2)));var pool=Executors.newFixedThreadPool(2);
        try{var upload=pool.submit(()->statusOnly(multipart("/api/venues/"+v+"/photos").file(mediaFile("webp")),owner,null));
            var update=pool.submit(()->statusOnly(put("/api/venues/"+v),owner,b));
            assertEquals(200,upload.get(30,TimeUnit.SECONDS));assertEquals(200,update.get(30,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();disarmBarrier();}
        var refs=(List<?>)body(v).get("photoPaths");assertEquals(reordered,refs.subList(0,2));assertTrue(refs.size()==2||refs.size()==3);
        assertEquals(count+1,mediaFiles()); // A stale full PUT can detach the new reference; no physical object deletion.
    }

    @Autowired MutableAvailabilityClock availabilityClock;
    @Autowired BookingService availabilityBookingService;
    @AfterEach void restoreAvailabilityClock(){availabilityClock.fixed=null;}
    static class MutableAvailabilityClock extends Clock {
        volatile Instant fixed;
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}
        @Override public Instant instant(){return fixed==null?Instant.now():fixed;}
    }
    Map<String,Object> avlRange(DayOfWeek day,String start,String end){return Map.of("dayOfWeek",day,"startTime",start,"endTime",end);}
    void avlWeekly(UUID v,List<?> rows,int expected)throws Exception{request(put("/api/venues/"+v+"/availability/weekly"),owner,Map.of("intervals",rows),expected);}
    List<String> avlIntervals(UUID v,LocalDate day)throws Exception{
        var result=request(get("/api/venues/"+v+"/availability").param("from",day.toString()).param("to",day.toString()),owner,null,200);
        var resultList=new ArrayList<String>();result.path("dates").get(0).path("intervals").forEach(n->resultList.add(n.path("startTime").asText()+"-"+n.path("endTime").asText()));return resultList;
    }
    UUID avlOverride(UUID v,LocalDate day,String type,boolean allDay,String start,String end,int expected)throws Exception{
        var body=new HashMap<String,Object>();body.put("date",day.toString());body.put("type",type);body.put("allDay",allDay);body.put("startTime",start);body.put("endTime",end);
        var result=request(post("/api/venues/"+v+"/availability/overrides"),owner,body,expected);return expected==201?uuid(result):null;
    }
    @ParameterizedTest @EnumSource(DayOfWeek.class)
    void avlWeeklyReplacementAllWeekdays(DayOfWeek day)throws Exception{
        UUID v=draft();LocalDate d=date.with(java.time.temporal.TemporalAdjusters.nextOrSame(day));
        avlWeekly(v,List.of(),200);assertEquals(List.of(),avlIntervals(v,d));
        avlWeekly(v,List.of(avlRange(day,"09:00","17:00")),200);assertEquals(List.of("09:00:00-17:00:00"),avlIntervals(v,d));
        avlWeekly(v,List.of(avlRange(day,"13:00","17:00"),avlRange(day,"09:00","12:00")),200);
        assertEquals(List.of("09:00:00-12:00:00","13:00:00-17:00:00"),avlIntervals(v,d));assertEquals(2,rows("venue_operating_hours","venue_id",v));
        avlWeekly(v,List.of(avlRange(day,"09:00","12:00"),avlRange(day,"12:00","15:00")),200);assertEquals(List.of("09:00:00-15:00:00"),avlIntervals(v,d));
        avlWeekly(v,List.of(),200);assertEquals(0,rows("venue_operating_hours","venue_id",v));assertEquals(List.of(),avlIntervals(v,d));
        assertEquals(0,audit(v).size());assertEquals(0,notifications(v));
    }
    @Test void avlWeeklyValidationAndAuthorizationAreAtomic()throws Exception{
        UUID v=draft();weekly(v);String route="/api/venues/"+v+"/availability";
        var before=request(get(route+"/settings"),owner,null,200);
        var valid=avlRange(DayOfWeek.MONDAY,"09:00","12:00");
        var invalid=new ArrayList<List<?>>();
        for(var pair:List.of(List.of("12:00","09:00"),List.of("09:00","09:00"),List.of("23:00","02:00")))invalid.add(List.of(avlRange(DayOfWeek.MONDAY,pair.get(0),pair.get(1))));
        for(String key:List.of("dayOfWeek","startTime","endTime")){var b=new HashMap<>(valid);b.remove(key);invalid.add(List.of(b));}
        invalid.add(List.of(valid,avlRange(DayOfWeek.MONDAY,"11:00","13:00")));invalid.add(Collections.nCopies(51,valid));
        for(var rows:invalid){avlWeekly(v,rows,400);assertEquals(before,request(get(route+"/settings"),owner,null,200));}
        request(put(route+"/weekly"),owner,Map.of(),400);
        for(var a:Arrays.asList(other,scout,null)){
            int code=a==null?401:403;request(get(route+"/settings"),a,null,403);
            request(put(route+"/weekly"),a,Map.of("intervals",List.of()),code);
            request(put(route+"/settings"),a,Map.of("timezone","Europe/Paris"),code);
            request(post(route+"/overrides"),a,Map.of("date",date,"type","BLOCKED","allDay",true),code);
            request(delete(route+"/overrides/"+UUID.randomUUID()),a,null,code);
        }
        jdbc.update("update users set email_verified=false where id=?",id(owner));
        try{request(get(route+"/settings"),owner,null,200);request(put(route+"/weekly"),owner,Map.of("intervals",List.of()),403);}finally{jdbc.update("update users set email_verified=true where id=?",id(owner));}
        assertEquals(before,request(get(route+"/settings"),owner,null,200));request(get(route+"/settings"),admin,null,200);
        request(put(route+"/weekly"),admin,Map.of("intervals",List.of()),200);
        var fifty=new ArrayList<Map<String,Object>>();
        for(int i=0;i<50;i++)fifty.add(avlRange(DayOfWeek.MONDAY,LocalTime.of(1,0).plusMinutes(i*2).toString(),LocalTime.of(1,0).plusMinutes(i*2+1).toString()));
        avlWeekly(v,fifty,200);assertEquals(50,rows("venue_operating_hours","venue_id",v));
        assertEquals(0,audit(v).size());assertEquals(0,notifications(v));
    }
    @Test void avlOverridesUnionBlockedPrecedenceDuplicatesAndDelete()throws Exception{
        UUID v=draft();avlWeekly(v,List.of(avlRange(date.getDayOfWeek(),"09:00","17:00")),200);
        avlOverride(v,date,"AVAILABLE",false,"08:00","10:00",201);avlOverride(v,date,"AVAILABLE",false,"16:00","18:00",201);
        UUID block=avlOverride(v,date,"BLOCKED",false,"11:00","12:00",201);avlOverride(v,date,"BLOCKED",false,"16:00","19:00",201);
        assertEquals(List.of("08:00:00-11:00:00","12:00:00-16:00:00"),avlIntervals(v,date));
        avlOverride(v,date,"AVAILABLE",false,"11:00","12:00",201);UUID duplicate=avlOverride(v,date,"BLOCKED",false,"11:00","12:00",201);
        request(delete("/api/venues/"+v+"/availability/overrides/"+block),owner,null,204);assertEquals(List.of("08:00:00-11:00:00","12:00:00-16:00:00"),avlIntervals(v,date));
        request(delete("/api/venues/"+v+"/availability/overrides/"+duplicate),owner,null,204);assertEquals(List.of("08:00:00-16:00:00"),avlIntervals(v,date));
        UUID v2=draft(),id2=avlOverride(v2,date,"BLOCKED",true,null,null,201);
        request(delete("/api/venues/"+v+"/availability/overrides/"+id2),owner,null,404);assertEquals(1,rows("venue_availability_overrides","venue_id",v2));
        long count=rows("venue_availability_overrides","venue_id",v);
        for(var pair:List.of(List.of("12:00","09:00"),List.of("09:00","09:00")))avlOverride(v,date,"AVAILABLE",false,pair.get(0),pair.get(1),400);
        avlOverride(v,date,"BLOCKED",true,"09:00","17:00",400);avlOverride(v,date,"BLOCKED",false,null,"17:00",400);avlOverride(v,date,"BLOCKED",false,"09:00",null,400);
        assertEquals(count,rows("venue_availability_overrides","venue_id",v));assertEquals(0,notifications(v));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void avlAllDayOverridesWithEmptyAndPopulatedWeek(boolean hasBase)throws Exception{
        UUID v=active();if(hasBase)weekly(v);else assertEquals(List.of(),avlIntervals(v,date));
        avlOverride(v,date,"AVAILABLE",true,null,null,201);assertEquals(List.of("00:00:00-23:59:59.999999999"),avlIntervals(v,date));
        avlOverride(v,date,"BLOCKED",true,null,null,201);assertEquals(List.of(),avlIntervals(v,date));assertEquals("ACTIVE",status(v));
    }
    @Test void avlQueryRangeVisibilityAndTimezoneChange()throws Exception{
        UUID v=active();weekly(v);String route="/api/venues/"+v+"/availability";
        assertEquals(93,request(get(route).param("from",date.toString()).param("to",date.plusDays(92).toString()),null,null,200).path("dates").size());
        for(var req:List.of(get(route).param("from",date.toString()).param("to",date.plusDays(93).toString()),get(route).param("from",date.toString()).param("to",date.minusDays(1).toString()),get(route).param("from",date.toString()),get(route).param("to",date.toString()),get(route).param("from","not-date").param("to",date.toString())))request(req,null,null,400);
        UUID booking=booking(v,10);String historical=bookingSnapshot(booking);
        for(String zone:List.of("America/New_York","Europe/Amsterdam")){request(put(route+"/settings"),owner,Map.of("timezone",zone),200);assertEquals(zone,request(get(route).param("from",date.toString()).param("to",date.toString()),null,null,200).path("timezone").asText());assertEquals(historical,bookingSnapshot(booking));}
        for(VenueStatus status:VenueStatus.values()){
            jdbc.update("update venues set status=?::venue_status where id=?",status.name(),v); // Visibility fixture only; lifecycle itself is proven by VEN.
            for(var a:Arrays.asList(null,scout,owner,other,admin))request(get(route).param("from",date.toString()).param("to",date.toString()),a,null,status==VenueStatus.ACTIVE||a==owner||a==admin?200:403);
        }
    }
    @ParameterizedTest @EnumSource(BookingStatus.class)
    void avlAllBookingStatusesMatchDatabaseExclusion(BookingStatus status)throws Exception{
        UUID v=active();avlWeekly(v,List.of(avlRange(date.getDayOfWeek(),"09:00","17:00")),200);
        var body=new HashMap<>(bookingBody(v,11));body.put("endTime","13:00");UUID a=uuid(request(post("/api/bookings"),scout,body,201)),b=uuid(request(post("/api/bookings"),scout,body,201));
        jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),a);
        boolean blocks=Set.of(BookingStatus.AWAITING_PAYMENT,BookingStatus.CONFIRMED).contains(status);
        assertEquals(blocks,BookingReservationPolicy.blocksAvailability(status));
        if(blocks)assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b));
        else jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);
        assertEquals(blocks?List.of("09:00:00-11:00:00","13:00:00-17:00:00"):List.of("09:00:00-17:00:00"),avlIntervals(v,date));
        jdbc.update("update bookings set start_time='09:00',end_time='17:00' where id=?",a);assertEquals(blocks?List.of():List.of("09:00:00-17:00:00"),avlIntervals(v,date));
        String constraint=jdbc.queryForObject("select pg_get_constraintdef(oid) from pg_constraint where conname='bookings_no_overlapping_reservations'",String.class);
        assertTrue(constraint.contains("AWAITING_PAYMENT"));assertTrue(constraint.contains("CONFIRMED"));assertTrue(constraint.contains("[)"));
        for(BookingStatus otherStatus:BookingStatus.values())if(!Set.of(BookingStatus.AWAITING_PAYMENT,BookingStatus.CONFIRMED).contains(otherStatus))assertFalse(constraint.contains("'"+otherStatus.name()+"'"));
    }
    @RepeatedTest(3) void avlConcurrentAcceptanceReservesOnceAndExpiryReopens()throws Exception{
        UUID v=active();weekly(v);UUID a=booking(v,11);
        JsonNode secondScout=actor("SCOUT");jdbc.update("update users set email_verified=true where id=?",id(secondScout));acceptPolicies(secondScout);
        var secondBody=new HashMap<>(bookingBody(v,11));secondBody.put("scoutContact",Map.of("name","QA Second Scout","email",email(secondScout)));
        UUID b=uuid(request(post("/api/bookings"),secondScout,secondBody,201));
        assertEquals(2,jdbc.queryForObject("select count(distinct scout_id) from bookings where id in (?,?)",Integer.class,a,b));
        assertEquals(List.of("08:00:00-20:00:00"),avlIntervals(v,date));
        LOCK_BARRIER.set(new LockBarrier(v,new CyclicBarrier(2)));var pool=Executors.newFixedThreadPool(2);
        try{var x=pool.submit(()->statusOnly(post("/api/bookings/"+a+"/accept"),owner,null));var y=pool.submit(()->statusOnly(post("/api/bookings/"+b+"/accept"),owner,null));assertEquals(List.of(200,409),Stream.of(x.get(30,TimeUnit.SECONDS),y.get(30,TimeUnit.SECONDS)).sorted().toList());}finally{disarmBarrier();pool.shutdownNow();}
        var statuses=jdbc.queryForList("select status::text from bookings where id in (?,?) order by status::text",String.class,a,b);assertEquals(List.of("AWAITING_PAYMENT","UNAVAILABLE"),statuses);
        assertEquals(List.of("08:00:00-11:00:00","12:00:00-20:00:00"),avlIntervals(v,date));
        request(post("/api/bookings"),scout,bookingBody(v,11),409); // stale calendar cannot create a request for newly reserved time
        UUID winner=jdbc.queryForObject("select id from bookings where id in (?,?) and status='AWAITING_PAYMENT'",UUID.class,a,b);
        availabilityBookingService.expireApproval(winner,OffsetDateTime.now().plusDays(3));assertEquals(List.of("08:00:00-20:00:00"),avlIntervals(v,date));
    }
    @Test void avlLeadExpiryCommitsForRequestAndInquiryButCheckoutIsExempt()throws Exception{
        Instant initial=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);availabilityClock.fixed=initial;
        var start=initial.plusSeconds(4*3600).atZone(ZoneId.of("Europe/Amsterdam"));LocalDate day=start.toLocalDate();String st=start.toLocalTime().toString(),end=start.plusHours(1).toLocalTime().toString();
        // Choose an ordered same-day fixture even when the current four-hour offset is near midnight.
        if(!start.toLocalTime().isBefore(start.plusHours(1).toLocalTime())){start=start.plusHours(2);day=start.toLocalDate();st=start.toLocalTime().toString();end=start.plusHours(1).toLocalTime().toString();}
        UUID v=active();jdbc.update("update venues set minimum_lead_time_minutes=120 where id=?",v);
        UUID availableOverride=avlOverride(v,day,"AVAILABLE",true,null,null,201);
        var b=new HashMap<>(bookingBody(v,9));b.put("shootDate",day.toString());b.put("startTime",st);b.put("endTime",end);
        UUID req=uuid(request(post("/api/bookings"),scout,b,201));
        request(delete("/api/venues/"+v+"/availability/overrides/"+availableOverride),owner,null,204);
        var inquiry=request(post("/api/availability-inquiries"),scout,Map.of("venueId",v,"requestedDate",day,"requestedStartTime",st,"requestedEndTime",end,"message","QA lead expiry"),201);
        UUID inq=uuid(inquiry);request(post("/api/availability-inquiries/"+inq+"/offer"),owner,Map.of("offeredDate",day,"offeredStartTime",st,"offeredEndTime",end),200);
        availabilityClock.fixed=start.toInstant().minusSeconds(3600);
        request(post("/api/bookings/"+req+"/accept"),owner,null,409);assertEquals("EXPIRED",jdbc.queryForObject("select status::text from bookings where id=?",String.class,req));
        assertEquals(1,events.stream(com.eema.backend.event.BookingLifecycleEvent.class).filter(e->e.bookingId().equals(req)&&e.type()==com.eema.backend.event.BookingLifecycleEvent.Type.REQUEST_EXPIRED).count());
        assertEquals("MINIMUM_LEAD_TIME_NOT_MET",jdbc.queryForObject("select payload->'data'->>'reason' from booking_events where booking_id=? and event_type='REQUEST_EXPIRED'",String.class,req));
        long count=rows("bookings","venue_id",v);request(post("/api/availability-inquiries/"+inq+"/accept"),scout,null,409);assertEquals("EXPIRED",jdbc.queryForObject("select status::text from availability_inquiries where id=?",String.class,inq));assertEquals(count,rows("bookings","venue_id",v));
        assertEquals(1,events.stream(com.eema.backend.event.AvailabilityInquiryLifecycleEvent.class).filter(e->e.inquiryId().equals(inq)&&e.type()==com.eema.backend.event.AvailabilityInquiryLifecycleEvent.Type.OFFER_EXPIRED).count());
        availabilityClock.fixed=initial;avlOverride(v,day,"AVAILABLE",true,null,null,201);UUID accepted=uuid(request(post("/api/bookings"),scout,b,201));request(post("/api/bookings/"+accepted+"/accept"),owner,null,200);
        availabilityClock.fixed=start.toInstant().minusSeconds(3600);
        assertTrue(availabilityClock.instant().isBefore(jdbc.queryForObject("select payment_expires_at from bookings where id=?",OffsetDateTime.class,accepted).toInstant()));
        when(mollie.createPayment(eq(accepted),any(),any())).thenReturn(new MolliePaymentResponse("tr_avl_qa","open",new MolliePaymentResponse.Amount("100.00","EUR"),new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/qa")),Map.of("bookingId",accepted.toString())));
        request(post("/api/bookings/"+accepted+"/checkout"),scout,null,201);assertEquals("AWAITING_PAYMENT",jdbc.queryForObject("select status::text from bookings where id=?",String.class,accepted));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void avlConfirmationKeepsReservationAndCancellationOrRefundReleasesIt(boolean refund)throws Exception{
        UUID v=active();weekly(v);UUID b=booking(v,11);request(post("/api/bookings/"+b+"/accept"),owner,null,200);
        String paymentId="tr_avl_"+b;String amount=jdbc.queryForObject("select venue_price_eur::text from bookings where id=?",String.class,b);
        var links=new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/qa"));
        when(mollie.createPayment(eq(b),any(),any())).thenReturn(new MolliePaymentResponse(paymentId,"open",new MolliePaymentResponse.Amount(amount,"EUR"),links,Map.of("bookingId",b.toString())));
        request(post("/api/bookings/"+b+"/checkout"),scout,null,201);
        availabilityBookingService.processPaidPayment(new MolliePaymentResponse(paymentId,"paid",new MolliePaymentResponse.Amount(amount,"EUR"),links,Map.of("bookingId",b.toString())));
        assertEquals("CONFIRMED",jdbc.queryForObject("select status::text from bookings where id=?",String.class,b));assertEquals(List.of("08:00:00-11:00:00","12:00:00-20:00:00"),avlIntervals(v,date));
        when(mollie.createRefund(eq(paymentId),eq(b),any(),any())).thenReturn(new com.eema.backend.dto.response.MollieRefundResponse("re_avl_"+b,"refunded",paymentId,new MolliePaymentResponse.Amount(amount,"EUR")));
        Object consent=refund?null:Map.of("previewToken",request(get("/api/bookings/"+b+"/cancellation-preview"),scout,null,200).path("previewToken").asText());
        request(post("/api/bookings/"+b+(refund?"/refund":"/cancel")),refund?admin:scout,consent,refund?202:200);
        assertEquals(refund?"REFUNDED":"CANCELLED",jdbc.queryForObject("select status::text from bookings where id=?",String.class,b));assertEquals(List.of("08:00:00-20:00:00"),avlIntervals(v,date));
    }
}
