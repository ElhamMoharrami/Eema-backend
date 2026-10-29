package com.eema.backend.integration;

import com.eema.backend.entity.enums.*;
import com.eema.backend.event.*;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class InquiryIntegrationTest extends InquirySupport {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    static Stream<Arguments> messages(){return Stream.of(Arguments.of(null,400),Arguments.of("",400),Arguments.of(" \t ",400),Arguments.of("é",201),Arguments.of("é".repeat(1000),201),Arguments.of("é".repeat(1001),400),Arguments.of("  QA trimmed  ",201));}
    @ParameterizedTest @MethodSource("messages")
    void messageContractAndCreationHaveNoInventoryWrites(String message,int expected)throws Exception{
        UUID v=inquiryVenue();var before=calendar(v);var effective=available(v);var body=inquiryBody(v);body.put("message",message);
        long events=hooks.committed.size();var result=req(post("/api/availability-inquiries"),scout,body,expected);
        assertEquals(before,calendar(v));assertEquals(effective,available(v));assertEquals(0,rows("bookings","venue_id",v));
        if(expected==201){UUID i=uuid(result);assertEquals("OPEN",inquiryState(i));assertEquals(message.trim(),result.path("message").asText());assertEquals("Europe/Amsterdam",result.path("timezone").asText());assertEquals(1,noticesFor(i));assertEquals(id(scout).toString(),result.path("scoutId").asText());}
        else {assertEquals(0,rows("availability_inquiries","venue_id",v));assertEquals(events,hooks.committed.size());}
    }
    @ParameterizedTest @ValueSource(strings={"equal","reverse","overnight","past","bad-date","bad-time","missing-start","missing-date","DRAFT","PENDING","SUSPENDED","email","policy","OWNER","ADMIN","anonymous"})
    void invalidCreationIsAtomic(String kind)throws Exception{
        UUID v=inquiryVenue();var body=inquiryBody(v);JsonNode actor=scout;int code=400;
        switch(kind){
            case "equal"->body.put("requestedEndTime","18:00");case "reverse"->body.put("requestedEndTime","17:00");case "overnight"->{body.put("requestedStartTime","23:00");body.put("requestedEndTime","02:00");}
            case "past"->{body.put("requestedDate",LocalDate.now().minusDays(1).toString());code=409;}
            case "bad-date"->body.put("requestedDate","2026-02-30");case "bad-time"->body.put("requestedStartTime","25:00");case "missing-start"->body.remove("requestedStartTime");case "missing-date"->body.remove("requestedDate");
            case "DRAFT","PENDING","SUSPENDED"->{jdbc.update("update venues set status=?::venue_status where id=?",kind,v);code=404;}
            case "email","policy"->{actor=actor("SCOUT");if(kind.equals("policy"))jdbc.update("update users set email_verified=true where id=?",id(actor));code=403;}
            case "OWNER"->{actor=owner;code=403;}case "ADMIN"->{actor=admin;code=403;}case "anonymous"->{actor=null;code=401;}
        }
        var before=calendar(v);long notices=hooks.committed.size();req(post("/api/availability-inquiries"),actor,body,code);
        assertEquals(0,rows("availability_inquiries","venue_id",v));assertEquals(0,rows("bookings","venue_id",v));assertEquals(before,calendar(v));assertEquals(notices,hooks.committed.size());
    }
    @Test void normalBookingVersusInquiryRoutingAndIgnoredStateInjection()throws Exception{
        UUID v=inquiryVenue();var b=inquiryBody(v);b.put("requestedStartTime","09:00");b.put("requestedEndTime","11:00");
        var error=req(post("/api/availability-inquiries"),scout,b,400);assertTrue(error.toString().contains("normal booking request"));UUID booking=create(v,scout,"09:00","11:00");req(post("/api/availability-inquiries"),scout,b,400);assertEquals("REQUESTED",state(booking));
        req(post("/api/bookings"),scout,body(v,"18:00","20:00"),409);var outside=inquiryBody(v);outside.put("status","ACCEPTED");outside.put("acceptedBookingId",booking);var created=req(post("/api/availability-inquiries"),scout,outside,201);assertEquals("OPEN",created.path("status").asText());assertTrue(created.path("acceptedBookingId").isNull());
        action(booking,"accept",owner,200);UUID occupied=uuid(req(post("/api/availability-inquiries"),second,b,201));offer(occupied,owner,date,"09:00","11:00",409);assertEquals("OPEN",inquiryState(occupied));
    }
    @ParameterizedTest @EnumSource(BookingStatus.class)
    void offerConflictsUseExactlyBlockingStatusesAndHalfOpenIntervals(BookingStatus status)throws Exception{
        UUID v=inquiryVenue(),b=create(v,scout,"09:00","12:00");jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);
        UUID i=open(v,second);boolean blocking=Set.of(BookingStatus.AWAITING_PAYMENT,BookingStatus.CONFIRMED).contains(status);
        offer(i,owner,date,"11:59","14:00",blocking?409:200);assertEquals(blocking?"OPEN":"OFFERED",inquiryState(i));
        if(blocking)offer(i,owner,date,"12:00","14:00",200);
        assertEquals(1,rows("bookings","venue_id",v));
    }
    @ParameterizedTest @ValueSource(ints={2,72})
    void offerExpiryIsCappedAndDifferentTimeIsHistorical(int hours)throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);var before=calendar(v);var offered=clock.instant().plusSeconds(hours*3600L).atZone(ZoneId.of("Europe/Amsterdam"));
        // Ordered same-day interval, independent of the wall clock hour.
        if(offered.getHour()==23){clock.now=clock.now.minusSeconds(2*3600);offered=clock.instant().plusSeconds(hours*3600L).atZone(ZoneId.of("Europe/Amsterdam"));}
        var dto=offer(i,owner,offered.toLocalDate(),offered.toLocalTime().toString(),offered.plusMinutes(30).toLocalTime().toString(),200);
        Instant expiry=clock.instant().plus(Duration.ofHours(offerProperties.offerValidHours()));if(offered.toInstant().isBefore(expiry))expiry=offered.toInstant();
        assertEquals(expiry,OffsetDateTime.parse(dto.path("offerExpiresAt").asText()).toInstant());assertEquals(date.toString(),dto.path("requestedDate").asText());assertEquals(before,calendar(v));assertEquals(0,rows("bookings","venue_id",v));
        String row=inquiryRow(i);offer(i,owner,date,"18:00","20:00",409);assertEquals(row,inquiryRow(i));assertEquals(2,noticesFor(i));
    }
    @ParameterizedTest @ValueSource(longs={0,-1})
    void createAndOfferRespectExactLeadBoundary(long seconds)throws Exception{
        UUID v=inquiryVenue();clock.now=date.atTime(17,0).atZone(ZoneId.of("Europe/Amsterdam")).toInstant();var b=inquiryBody(v);b.put("requestedStartTime",LocalTime.of(18,0).plusSeconds(seconds).toString());
        req(post("/api/availability-inquiries"),scout,b,seconds==0?201:409);UUID i=open(v,scout);
        offer(i,owner,date,LocalTime.of(18,0).plusSeconds(seconds).toString(),"20:00",seconds==0?200:409);
    }
    @Test void conversionUsesAcceptancePriceContactsPoliciesAndStandardCheckout()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);var before=calendar(v);offer(i,owner,date,"09:15","13:45",200);var offeredAvailability=available(v);
        jdbc.update("update venues set hourly_rate_eur=150,minimum_booking_minutes=360,full_day_rate_eur=800 where id=?",v);
        jdbc.update("update users set full_name='QA Inquiry Contact',phone='+31000000001' where id in (?,?)",id(scout),id(owner));
        var booking=inquiryAction(i,"accept",scout,200);UUID b=uuid(booking);var quote=calculator.calculate(new BigDecimal("150"),360,new BigDecimal("800"),LocalTime.of(9,15),LocalTime.of(13,45));
        assertEquals("ACCEPTED",inquiryState(i));assertEquals(b,jdbc.queryForObject("select accepted_booking_id from availability_inquiries where id=?",UUID.class,i));assertEquals("AWAITING_PAYMENT",booking.path("status").asText());assertTrue(booking.path("ownerResponseExpiresAt").isNull());assertEquals(270,booking.path("durationMinutes").asInt());assertEquals(360,booking.path("billableMinutes").asInt());assertEquals(0,quote.finalPriceEur().compareTo(booking.path("venuePriceEur").decimalValue()));
        assertEquals(clock.instant().plus(Duration.ofHours(windows.paymentWindowHours())),OffsetDateTime.parse(booking.path("paymentExpiresAt").asText()).toInstant());assertEquals(0,app.commission().percentage().compareTo(booking.path("commissionPercentage").decimalValue()));assertEquals("QA Inquiry Contact",booking.path("scoutContact").path("name").asText());assertEquals(email(scout),booking.path("scoutContact").path("email").asText());assertEquals("QA Inquiry Contact",booking.path("ownerContact").path("name").asText());
        assertEquals(4,rows("booking_policy_versions","booking_id",b));assertFalse(references(b).toString().contains("PRIVACY"));assertEquals(List.of("REQUEST_ACCEPTED"),audit(b));assertTrue(jdbc.queryForObject("select payload::text from booking_events where booking_id=?",String.class,b).contains("availability_inquiry"));assertEquals(1,hooks.committedBookings.stream().filter(e->e.bookingId().equals(b)).count());assertEquals(2,noticesFor(i));assertEquals(before,calendar(v));assertNotEquals(offeredAvailability,available(v));
        inquiryAction(i,"accept",scout,409);assertEquals(1,rows("bookings","venue_id",v));
        jdbc.update("update venues set hourly_rate_eur=999 where id=?",v);assertEquals(booking.path("venuePriceEur"),detail(b).path("venuePriceEur"));
        checkoutExercised=true;when(mollie.createPayment(eq(b),any(),any())).thenReturn(new MolliePaymentResponse("tr_inq_qa","open",new MolliePaymentResponse.Amount(quote.finalPriceEur().toPlainString(),"EUR"),new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://checkout.example.test/inq")),Map.of("bookingId",b.toString())));
        req(post("/api/bookings/"+b+"/checkout"),scout,null,201);verify(mollie).createPayment(eq(b),eq(quote.finalPriceEur()),any());assertEquals("AWAITING_PAYMENT",state(b));
    }
    @ParameterizedTest @ValueSource(longs={-1,0,1})
    void deadlineEqualityDoesNotRelyOnWorker(long microseconds)throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);var dto=offer(i,owner,date,"18:00","20:00",200);clock.now=OffsetDateTime.parse(dto.path("offerExpiresAt").asText()).toInstant().plusNanos(microseconds*1000);
        inquiryAction(i,"accept",scout,microseconds<0?200:409);assertEquals(microseconds<0?"ACCEPTED":"OFFERED",inquiryState(i));assertEquals(microseconds<0?1:0,rows("bookings","venue_id",v));
        if(microseconds>=0){assertEquals(2,noticesFor(i));inquiryService.expireOffers(OffsetDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));assertEquals("EXPIRED",inquiryState(i));assertEquals(3,noticesFor(i));}
    }
    @Test void leadExpiryCommitsButOccupiedOrSuspendedOfferRemainsOffered()throws Exception{
        for(String condition:List.of("lead","occupied","suspended")){
            clock.now=date.atTime(15,0).atZone(ZoneId.of("Europe/Amsterdam")).toInstant();UUID v=inquiryVenue(),i=open(v,scout);var originalCalendar=calendar(v);offer(i,owner,date,"18:00","20:00",200);
            if(condition.equals("lead"))clock.now=date.atTime(17,30).atZone(ZoneId.of("Europe/Amsterdam")).toInstant();
            else if(condition.equals("suspended"))req(post("/api/admin/venues/"+v+"/suspend"),admin,Map.of("reason","QA suspension"),200);
            else {UUID competitor=open(v,second);offer(competitor,owner,date,"18:00","20:00",200);inquiryAction(competitor,"accept",second,200);}
            long count=rows("bookings","venue_id",v);inquiryAction(i,"accept",scout,condition.equals("suspended")?404:409);assertEquals(condition.equals("lead")?"EXPIRED":"OFFERED",inquiryState(i));assertEquals(count,rows("bookings","venue_id",v));assertEquals(originalCalendar,calendar(v));
        }
    }
    @ParameterizedTest @ValueSource(strings={"offer","decline","accept"})
    void actorsAndReadPrivacyAreStrictlyScoped(String operation)throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);if(operation.equals("accept"))offer(i,owner,date,"18:00","20:00",200);String original=inquiryRow(i);long events=noticesFor(i);
        var denied=operation.equals("accept")?List.of(second,owner,other,admin):List.of(scout,second,other,admin);
        for(var a:denied){if(operation.equals("offer"))offer(i,a,date,"18:00","20:00",403);else inquiryAction(i,operation,a,403);assertEquals(original,inquiryRow(i));}
        if(operation.equals("offer"))offer(i,null,date,"18:00","20:00",401);else inquiryAction(i,operation,null,401);assertEquals(events,noticesFor(i));
        for(var a:List.of(scout,second,owner,other,admin)){
            boolean isScout=a==scout||a==second,isOwner=a==owner||a==other;
            var mine=req(get("/api/availability-inquiries/mine"),a,null,isScout?200:403);var owned=req(get("/api/availability-inquiries/owned-venues"),a,null,isOwner?200:403);
            if(isScout)assertEquals(a==scout,mine.toString().contains(i.toString()));if(isOwner)assertEquals(a==owner,owned.toString().contains(i.toString()));
        }
        var serialized=req(get("/api/availability-inquiries/owned-venues"),owner,null,200).toString();for(String key:List.of("email","phone","iban","scoutContact","ownerContact","molliePaymentId","password"))assertFalse(serialized.contains('"'+key+'"'));
        req(get("/api/availability-inquiries/mine"),null,null,401);inquiryAction(UUID.randomUUID(),"decline",owner,404);req(post("/api/availability-inquiries/not-uuid/decline"),owner,null,400);
    }
    static Stream<Arguments> states(){return Arrays.stream(AvailabilityInquiryStatus.values()).flatMap(s->Stream.of("offer","decline","accept","expire").map(op->Arguments.of(s,op)));}
    @ParameterizedTest @MethodSource("states")
    void allFiveStatesAndFourActions(AvailabilityInquiryStatus status,String operation)throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);if(status==AvailabilityInquiryStatus.DECLINED)inquiryAction(i,"decline",owner,200);
        else if(status!=AvailabilityInquiryStatus.OPEN){var offered=offer(i,owner,date,"18:00","20:00",200);if(status==AvailabilityInquiryStatus.ACCEPTED)inquiryAction(i,"accept",scout,200);if(status==AvailabilityInquiryStatus.EXPIRED)inquiryService.expireOffers(OffsetDateTime.parse(offered.path("offerExpiresAt").asText()));}
        String before=inquiryRow(i);var schedule=calendar(v);long notices=noticesFor(i),bookingCount=rows("bookings","venue_id",v);
        boolean allowed=(operation.equals("offer")||operation.equals("decline"))?status==AvailabilityInquiryStatus.OPEN:status==AvailabilityInquiryStatus.OFFERED;
        if(operation.equals("expire"))inquiryService.expireOffers(OffsetDateTime.ofInstant(clock.instant().plus(Duration.ofHours(49)),ZoneOffset.UTC));
        else if(operation.equals("offer"))offer(i,owner,date,"18:00","20:00",allowed?200:409);else inquiryAction(i,operation,operation.equals("accept")?scout:owner,allowed?200:409);
        if(!allowed){assertEquals(before,inquiryRow(i));assertEquals(notices,noticesFor(i));assertEquals(bookingCount,rows("bookings","venue_id",v));}
        else assertEquals(switch(operation){case "offer"->"OFFERED";case "decline"->"DECLINED";case "accept"->"ACCEPTED";default->"EXPIRED";},inquiryState(i));assertEquals(schedule,calendar(v));
    }
    @RepeatedTest(3) void expiryWorkerIsDurableAndIdempotent()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);var dto=offer(i,owner,date,"18:00","20:00",200);var before=calendar(v);OffsetDateTime due=OffsetDateTime.parse(dto.path("offerExpiresAt").asText());inquiryService.expireOffers(due);inquiryService.expireOffers(due);
        assertEquals("EXPIRED",inquiryState(i));assertEquals(3,noticesFor(i));assertEquals(0,rows("bookings","venue_id",v));assertEquals(before,calendar(v));
    }
    @Test void inquiryWriteFailureRollsBackBookingPoliciesAuditAndAfterCommitNotice()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);offer(i,owner,date,"18:00","20:00",200);var before=inquiryRow(i);var schedule=calendar(v);long notices=hooks.committedBookings.size();
        // Add a test-only stricter constraint in disposable PostgreSQL. No production invariant is disabled.
        jdbc.execute("alter table availability_inquiries add constraint qa_fail_"+i.toString().replace("-","")+" check (id <> '"+i+"'::uuid or status <> 'ACCEPTED')");
        inquiryAction(i,"accept",scout,500);assertEquals(before,inquiryRow(i));assertEquals(0,rows("bookings","venue_id",v));assertEquals(notices,hooks.committedBookings.size());assertEquals(schedule,calendar(v));
    }
    @RepeatedTest(3) void concurrentSameOfferCreatesExactlyOneBooking()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);offer(i,owner,date,"18:00","20:00",200);gate=new Gate("inquiry",Set.of(i));var pool=Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->httpAccept(i,scout));var b=pool.submit(()->httpAccept(i,scout));assertEquals(List.of(200,409),Stream.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)).sorted().toList());assertEquals(2,gate.arrivals().get());}
        finally{pool.shutdownNow();gate=null;}
        assertEquals("ACCEPTED",inquiryState(i));assertEquals(1,rows("bookings","venue_id",v));assertEquals(1,jdbc.queryForObject("select count(*) from booking_events e join bookings b on b.id=e.booking_id where b.venue_id=?",Integer.class,v));assertEquals(2,noticesFor(i));
    }
    @RepeatedTest(3) void twoOffersAndNormalAcceptanceShareVenueReservationBoundary()throws Exception{
        for(boolean normal:List.of(false,true)){
            UUID v=inquiryVenue(),a=open(v,scout);offer(a,owner,date,"11:00","13:00",200);UUID b;
            if(normal)b=create(v,second,"11:00","13:00");else {b=open(v,second);offer(b,owner,date,"11:00","13:00",200);}
            gate=new Gate("venue",Set.of(v));var pool=Executors.newFixedThreadPool(2);
            try{
                var first=pool.submit(()->httpAccept(a,scout));var secondResult=pool.submit(()->normal?mvc.perform(post("/api/bookings/"+b+"/accept").contextPath("/api").header("Authorization","Bearer "+access(owner))).andReturn().getResponse().getStatus():httpAccept(b,second));
                assertEquals(List.of(200,409),Stream.of(first.get(30,TimeUnit.SECONDS),secondResult.get(30,TimeUnit.SECONDS)).sorted().toList());assertTrue(gate.arrivals().get()>=2);
            }finally{pool.shutdownNow();gate=null;}
            assertEquals(1,jdbc.queryForObject("select count(*) from bookings where venue_id=? and status='AWAITING_PAYMENT'",Integer.class,v));assertEquals(0,jdbc.queryForObject("select count(*) from availability_inquiries i left join bookings b on b.id=i.accepted_booking_id where i.venue_id=? and i.status='ACCEPTED' and b.id is null",Integer.class,v));
        }
    }
    @RepeatedTest(3) void suspensionAndInquiryAcceptanceSerialize()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);offer(i,owner,date,"18:00","20:00",200);gate=new Gate("venue",Set.of(v));var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->httpAccept(i,scout));var b=pool.submit(()->req(post("/api/admin/venues/"+v+"/suspend"),admin,Map.of("reason","QA concurrent suspension"),200));int accepted=a.get(30,TimeUnit.SECONDS);b.get(30,TimeUnit.SECONDS);assertTrue(Set.of(200,404).contains(accepted));assertEquals(accepted==200?"ACCEPTED":"OFFERED",inquiryState(i));assertEquals(accepted==200?1:0,rows("bookings","venue_id",v));assertTrue(gate.arrivals().get()>=2);}
        finally{pool.shutdownNow();gate=null;}
        assertEquals("SUSPENDED",jdbc.queryForObject("select status::text from venues where id=?",String.class,v));
    }
    @Test void suspendedOwnerOfferCannotMutateNegotiation()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);req(post("/api/admin/venues/"+v+"/suspend"),admin,Map.of("reason","QA suspension"),200);String before=inquiryRow(i);offer(i,owner,date,"18:00","20:00",409);assertEquals(before,inquiryRow(i));assertEquals(1,noticesFor(i));
    }
    @ParameterizedTest @ValueSource(strings={"missing-date","missing-start","missing-end","equal","reverse","past","bad-time"})
    void invalidOfferNeverChangesOpenInquiry(String kind)throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);var body=new HashMap<String,Object>(offerBody(date,"18:00","20:00"));int code=400;
        switch(kind){case "missing-date"->body.remove("offeredDate");case "missing-start"->body.remove("offeredStartTime");case "missing-end"->body.remove("offeredEndTime");case "equal"->body.put("offeredEndTime","18:00");case "reverse"->body.put("offeredEndTime","17:00");case "past"->{body.put("offeredDate",date.minusDays(30).toString());code=409;}case "bad-time"->body.put("offeredStartTime","25:00");}
        String before=inquiryRow(i);req(post("/api/availability-inquiries/"+i+"/offer"),owner,body,code);assertEquals(before,inquiryRow(i));assertEquals(1,noticesFor(i));assertEquals(0,rows("bookings","venue_id",v));
    }
    static Stream<Arguments> trustChanges(){return Stream.of("offer","decline","accept").flatMap(op->Stream.of("email","inactive").map(change->Arguments.of(op,change)));}
    @ParameterizedTest @MethodSource("trustChanges")
    void mutationUsesCurrentAccountAuthorityNotOldToken(String op,String change)throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);if(op.equals("accept"))offer(i,owner,date,"18:00","20:00",200);
        JsonNode actor=op.equals("accept")?scout:owner;String column=change.equals("email")?"email_verified":"active";String before=inquiryRow(i);long count=noticesFor(i);
        jdbc.update("update users set "+column+"=false where id=?",id(actor));
        try{int denied=change.equals("inactive")?401:403;if(op.equals("offer"))offer(i,actor,date,"18:00","20:00",denied);else inquiryAction(i,op,actor,denied);assertEquals(before,inquiryRow(i));assertEquals(count,noticesFor(i));assertEquals(0,rows("bookings","venue_id",v));}
        finally{jdbc.update("update users set "+column+"=true where id=?",id(actor));}
    }
    @Test void policyReplacementGatesOfferDeclineAndAcceptanceThenSnapshotsReplacement()throws Exception{
        UUID v=inquiryVenue(),accepted=open(v,scout),offered=open(v,scout),declined=open(v,scout);offer(accepted,owner,date,"18:00","20:00",200);
        var draft=req(post("/api/admin/policies"),admin,Map.of("policyType","PRODUCTION_POLICY","version","QA-INQ-"+UUID.randomUUID(),"title","QA TEST CONTENT — NOT LEGAL TERMS","content","QA TEST CONTENT — NOT LEGAL TERMS","effectiveAt",OffsetDateTime.now().toString()),201);
        UUID replacement=uuid(draft);req(post("/api/admin/policies/"+replacement+"/publish"),admin,null,200);
        try{offer(offered,owner,date,"18:00","20:00",403);inquiryAction(declined,"decline",owner,403);inquiryAction(accepted,"accept",scout,403);assertEquals("OFFERED",inquiryState(accepted));assertEquals("OPEN",inquiryState(offered));assertEquals(0,rows("bookings","venue_id",v));}
        finally{policies.clear();policies.addAll(jdbc.queryForList("select id from policy_versions where status='PUBLISHED'",UUID.class));for(var actor:List.of(owner,other,scout,second))acceptPolicies(actor);}
        UUID booking=uuid(inquiryAction(accepted,"accept",scout,200));assertTrue(references(booking).toString().contains(replacement.toString()));
    }

    @Test void waitingForVenueLockCannotExtendOfferDeadline()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);var dto=offer(i,owner,date,"18:00","20:00",200);Instant due=OffsetDateTime.parse(dto.path("offerExpiresAt").asText()).toInstant();clock.now=due.minusSeconds(1);
        var pool=Executors.newSingleThreadExecutor();var attempt=new java.util.concurrent.atomic.AtomicReference<Future<Integer>>();
        try{
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status->{
                jdbc.queryForObject("select id from venues where id=? for update",UUID.class,v);
                awaitingVenue=v;venueAttempt=new CountDownLatch(1);attempt.set(pool.submit(()->httpAccept(i,scout)));
                try{assertTrue(venueAttempt.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                clock.now=due;
            });
            assertEquals(409,attempt.get().get(20,TimeUnit.SECONDS));assertEquals("OFFERED",inquiryState(i));assertEquals(0,rows("bookings","venue_id",v));assertEquals(2,noticesFor(i));
        }finally{pool.shutdownNow();awaitingVenue=null;venueAttempt=null;}
    }

    @Test void databaseRejectsBrokenReferencesIntervalsStatusesAndDuplicateBookingLink()throws Exception{
        UUID v=inquiryVenue(),i=open(v,scout);String before=inquiryRow(i);
        for(String assignment:List.of("venue_id='"+UUID.randomUUID()+"'::uuid","scout_id='"+UUID.randomUUID()+"'::uuid","status='CANCELLED'","requested_end_time=requested_start_time","status='OFFERED'","accepted_booking_id='"+UUID.randomUUID()+"'::uuid")){
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update availability_inquiries set "+assignment+" where id=?",i));assertEquals(before,inquiryRow(i));
        }
        offer(i,owner,date,"18:00","20:00",200);UUID booking=uuid(inquiryAction(i,"accept",scout,200));UUID otherInquiry=open(inquiryVenue(),second);String original=inquiryRow(otherInquiry);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update availability_inquiries set accepted_booking_id=? where id=?",booking,otherInquiry));assertEquals(original,inquiryRow(otherInquiry));
    }

}
