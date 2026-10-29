package com.eema.backend.integration;

import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.service.*;
import com.eema.backend.service.ai.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Approved EDGE v1 temporal, recall and deadline regressions over real PostgreSQL. */
@org.springframework.context.annotation.Import(EdgeIntegrationTest.DeadlineHooks.class)
class EdgeIntegrationTest extends InquirySupport {
    @org.springframework.boot.test.mock.mockito.SpyBean VenueFilterBuilder filter;
    @Autowired VenueMatchAvailabilityFilter matchingAvailability;
    @Autowired VenueBookingLeadTimePolicy lead;
    @MockBean GeminiClient gemini;
    @BeforeAll static void freshActors(){owner=null;policies.clear();}
    @AfterEach void noPaymentTransport(){verifyNoInteractions(mollie);}

    @ParameterizedTest @CsvSource({"09:00,09:00:59.999999999,400,0", "09:00:00.123456,09:01:00.123456,201,1", "09:00:30,09:01:59,201,1"})
    void secondsAndNanosRespectWholeMinuteGuard(String start,String end,int expected,int duration)throws Exception {
        UUID v=venue(owner,1);long before=events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count();
        var result=req(post("/api/bookings"),scout,body(v,start,end),expected);
        assertEquals(expected==201?1:0,rows("bookings","venue_id",v));
        if(expected==400){assertEquals(before,events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count());assertTrue(result.path("message").asText().contains("at least one minute"));}
        // Current Hibernate/JDBC TIME binding retains milliseconds; no nanosecond persistence promise.
        else {UUID b=uuid(result);assertEquals(duration,detail(b).path("durationMinutes").asInt());assertEquals(LocalTime.parse(start).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),LocalTime.parse(jdbc.queryForObject("select start_time::text from bookings where id=?",String.class,b)));assertEquals(LocalTime.parse(end).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),LocalTime.parse(jdbc.queryForObject("select end_time::text from bookings where id=?",String.class,b)));assertEquals("REQUESTED",state(b));}
    }

    @Test void midnightRoundingCarriesDateThroughPublicAvailability()throws Exception {
        UUID v=venue(owner,1);date=LocalDate.of(2027,1,12);clock.now=Instant.parse("2027-01-12T21:59:30Z"); // +60m ->23:59:30 Amsterdam
        jdbc.update("update venue_operating_hours set start_time='00:00',end_time='23:59:59' where venue_id=?",v);
        var result=available(v);assertTrue(result.toString().contains("\"intervals\":[]"),result.toString());
        req(post("/api/bookings"),scout,body(v,"23:58","23:59"),409);assertEquals(0,rows("bookings","venue_id",v));
        date=date.plusDays(1);UUID b=create(v,scout,"00:00","00:01");assertEquals("REQUESTED",state(b));
    }

    @ParameterizedTest @CsvSource({"2026-10-25,02:30,03:30,2026-10-25T00:30:00Z,2026-10-25T02:30:00Z,60,120"})
    void overlapConfirmationUsesEarlierOffsetAndWallClockPrice(String day,String start,String end,String startInstant,String endInstant,int wallMinutes,int elapsedMinutes)throws Exception {
        date=LocalDate.parse(day);clock.now=date.minusDays(14).atStartOfDay(ZoneOffset.UTC).toInstant();UUID v=venue(owner,1);
        jdbc.update("update venue_operating_hours set start_time='00:00',end_time='06:00' where venue_id=?",v);
        assertTrue(available(v).toString().contains("06:00"));
        UUID b=create(v,scout,start,end);action(b,"accept",owner,200);
        String pid="tr_edge_"+b;BigDecimal price=detail(b).path("venuePriceEur").decimalValue();
        // Attach a synthetic provider association; execute real confirmation without provider transport.
        jdbc.update("update bookings set mollie_payment_id=? where id=?",pid,b);
        var paid=new MolliePaymentResponse(pid,"paid",new MolliePaymentResponse.Amount(price.setScale(2).toPlainString(),"EUR"),null,Map.of("bookingId",b.toString()));
        assertEquals(BookingService.PaidPaymentResult.APPLIED,bookings.processPaidPayment(paid).result());
        assertEquals("CONFIRMED",state(b));assertEquals(wallMinutes,detail(b).path("durationMinutes").asInt());
        Instant resolvedStart=lead.bookingStart(venues.findById(v).orElseThrow(),date,LocalTime.parse(start));assertEquals(Instant.parse(startInstant),resolvedStart);
        Instant eligible=jdbc.queryForObject("select eligible_at from owner_payouts where booking_id=?",OffsetDateTime.class,b).toInstant();assertEquals(Instant.parse(endInstant),eligible);
        assertEquals(elapsedMinutes,Duration.between(resolvedStart,eligible).toMinutes());
        assertEquals(resolvedStart.minus(Duration.ofDays(7)),jdbc.queryForObject("select full_refund_until from bookings where id=?",OffsetDateTime.class,b).toInstant());
        assertEquals(resolvedStart.minus(Duration.ofHours(48)),jdbc.queryForObject("select partial_refund_until from bookings where id=?",OffsetDateTime.class,b).toInstant());
        var preview=req(get("/api/bookings/"+b+"/cancellation-preview"),scout,null,200);assertEquals(0,price.compareTo(preview.path("refundAmountEur").decimalValue()));
    }

    @RepeatedTest(3) void springGapCannotAliasReservations()throws Exception {
        date=LocalDate.of(2027,3,28);clock.now=Instant.parse("2027-03-14T00:00:00Z");UUID v=venue(owner,1);
        jdbc.update("update venue_operating_hours set start_time='00:00',end_time='06:00' where venue_id=?",v);
        long before=events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count();
        for(String[] times:List.of(new String[]{"02:00","02:30"},new String[]{"02:30","03:00"},new String[]{"01:30","02:30"})) {
            var error=req(post("/api/bookings"),scout,body(v,times[0],times[1]),400);
            assertTrue(error.path("message").asText().contains("clocks change"));
        }
        assertEquals(0,rows("bookings","venue_id",v));assertEquals(before,events.stream(com.eema.backend.event.BookingLifecycleEvent.class).count());
        UUID real=create(v,second,"03:00","03:30");action(real,"accept",owner,200);
        assertEquals(1,jdbc.queryForObject("select count(*) from bookings where venue_id=? and status='AWAITING_PAYMENT'",Integer.class,v));
        // Old/corrupt rows must also be rejected before becoming a reservation or provider claim.
        UUID legacy=create(v,scout,"04:00","04:30");jdbc.update("update bookings set start_time='02:00',end_time='02:30' where id=?",legacy);
        String unchanged=row(legacy);action(legacy,"accept",owner,400);assertEquals(unchanged,row(legacy));
        jdbc.update("update bookings set status='AWAITING_PAYMENT',payment_expires_at=? where id=?",OffsetDateTime.now(clock).plusHours(1),legacy);
        unchanged=row(legacy);req(post("/api/bookings/"+legacy+"/checkout"),scout,null,400);assertEquals(unchanged,row(legacy));
    }

    @Test void suspendedRequestDenialIsAtomic()throws Exception {
        UUID v=venue(owner,60),b=create(v,scout,"09:00","10:00");
        req(post("/api/admin/venues/"+v+"/suspend"),admin,Map.of("reason","QA EDGE moderation"),200);
        String before=row(b);long n=notices(b);action(b,"accept",owner,409);assertEquals(before,row(b));assertEquals(n,notices(b));assertEquals(List.of("REQUEST_SUBMITTED"),audit(b));
    }

    @ParameterizedTest @ValueSource(longs={-1,0,1})
    void declineDeadlineIsAuthoritative(long microseconds)throws Exception {
        UUID v=venue(owner,60),b=create(v,scout,"09:00","10:00");Instant deadline=Instant.parse(detail(b).path("ownerResponseExpiresAt").asText());clock.now=deadline.plusNanos(microseconds*1000);
        String expected=microseconds<0?"DECLINED":"EXPIRED";
        action(b,"decline",owner,microseconds<0?200:409);assertEquals(expected,state(b));assertEquals(List.of("REQUEST_SUBMITTED",microseconds<0?"REQUEST_DECLINED":"REQUEST_EXPIRED"),audit(b));assertEquals(2,notices(b));
        bookings.expireDueRequests(OffsetDateTime.ofInstant(deadline.plusSeconds(1),ZoneOffset.UTC),100);assertEquals(expected,state(b));assertEquals(2,notices(b));
        action(b,"decline",owner,409);assertEquals(2,notices(b));
    }
    @Test void workerFirstMakesOverdueDeclineConflict()throws Exception {
        UUID v=venue(owner,60),b=create(v,scout,"09:00","10:00");clock.now=Instant.parse(detail(b).path("ownerResponseExpiresAt").asText());
        bookings.expireDueRequests(OffsetDateTime.now(clock),100);assertEquals("EXPIRED",state(b));long n=notices(b);action(b,"decline",owner,409);assertEquals(n,notices(b));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_EXPIRED"),audit(b));
    }

    @Test void matchingContinuesToAvailableThirtyFirst()throws Exception {
        String district="QA-EDGE-"+UUID.randomUUID();UUID available=null;var ordered=new ArrayList<UUID>();
        for(int i=0;i<31;i++){UUID v=venue(owner,60);ordered.add(v);jdbc.update("update venues set neighbourhood=?,hourly_rate_eur=?,tags=ARRAY['studio']::text[] where id=?",district,10+i,v);if(i<30)jdbc.update("delete from venue_operating_hours where venue_id=?",v);else available=v;}
        var parsed=json.convertValue(Map.of("neighbourhood",district.toLowerCase(Locale.ROOT),"sizeM2Min",100,"mustHaveTags",List.of("studio"),"requestedDate",date.toString(),"requestedStartTime","09:00","requestedEndTime","10:00"),ParsedBrief.class);
        var selected=filter.findCandidateVenues(parsed);assertEquals(ordered.subList(0,30),selected.stream().map(com.eema.backend.entity.Venue::getId).toList());assertTrue(matchingAvailability.retainAvailable(selected,parsed).isEmpty());
        assertEquals(List.of(available),matchingAvailability.retainAvailable(List.of(venues.findById(available).orElseThrow()),parsed).stream().map(com.eema.backend.entity.Venue::getId).toList());
        when(gemini.generateContent(anyString())).thenReturn("edge-parser","edge-ranker");when(gemini.extractGeneratedText("edge-parser")).thenReturn(json.writeValueAsString(parsed));
        when(gemini.extractGeneratedText("edge-ranker")).thenReturn("[{\"venueId\":\""+available+"\",\"score\":0.9,\"explanation\":\"QA available\"}]");
        var result=req(post("/api/match"),scout,Map.of("brief","QA edge exact available studio search"),200);assertEquals(1,result.path("totalMatches").asInt());assertEquals(available.toString(),result.at("/venues/0/venue/id").asText());
        var prompts=org.mockito.ArgumentCaptor.forClass(String.class);verify(gemini,times(2)).generateContent(prompts.capture());
        assertTrue(prompts.getAllValues().get(1).contains(available.toString()));for(UUID id:ordered.subList(0,30))assertFalse(prompts.getAllValues().get(1).contains(id.toString()));
        assertEquals(0,jdbc.queryForObject("select count(*) from bookings b join venues v on v.id=b.venue_id where v.neighbourhood=?",Integer.class,district));
    }

    @Test void unsupportedCompatibilityMutationsDoNotPersist()throws Exception {
        UUID v=venue(owner,60),b=create(v,scout,"09:00","10:00");String before=row(b);long notices=notices(b);
        for(var a:List.of(owner,admin))req(delete("/api/venues/"+v),a,null,405);
        req(post("/api/bookings/"+b+"/force-confirm"),admin,Map.of(),404);
        req(put("/api/bookings/"+b+"/status"),admin,Map.of("status","CONFIRMED"),404);
        assertTrue(venues.existsById(v));assertEquals(before,row(b));assertEquals(notices,notices(b));
    }
    @Test void weeklyAndOverrideAvailabilityExcludeGapSelections()throws Exception {
        date=LocalDate.of(2027,3,28);clock.now=Instant.parse("2027-03-14T00:00:00Z");UUID v=venue(owner,1);
        jdbc.update("update venue_operating_hours set start_time='00:00',end_time='06:00' where venue_id=?",v);
        assertEquals(2,available(v).at("/dates/0/intervals").size());
        jdbc.update("delete from venue_operating_hours where venue_id=?",v);
        jdbc.update("insert into venue_availability_overrides(id,venue_id,override_date,override_type,all_day) values (?,?,?,'AVAILABLE',true)",UUID.randomUUID(),v,date);
        var spans=available(v).at("/dates/0/intervals");assertEquals(2,spans.size());
        assertEquals("03:00:00",spans.get(1).path("startTime").asText());
        var brief=json.convertValue(Map.of("requestedDate",date.toString(),"requestedStartTime","02:00","requestedEndTime","02:30"),ParsedBrief.class);
        assertTrue(matchingAvailability.retainAvailable(List.of(venues.findById(v).orElseThrow()),brief).isEmpty());
    }

    @RepeatedTest(3) void inquiryGapCreationOfferAndAcceptanceAreAtomic()throws Exception {
        date=LocalDate.of(2027,3,28);clock.now=Instant.parse("2027-03-14T00:00:00Z");UUID v=inquiryVenue();
        var body=inquiryBody(v);body.put("requestedStartTime","02:00");body.put("requestedEndTime","02:30");
        req(post("/api/availability-inquiries"),scout,body,400);assertEquals(0,rows("availability_inquiries","venue_id",v));
        UUID i=open(v,scout);String before=inquiryRow(i);long n=noticesFor(i);
        offer(i,owner,date,"02:00","02:30",400);assertEquals(before,inquiryRow(i));assertEquals(n,noticesFor(i));
        offer(i,owner,date,"03:00","03:30",200);
        jdbc.update("update availability_inquiries set offered_start_time='02:00',offered_end_time='02:30' where id=?",i);
        before=inquiryRow(i);n=noticesFor(i);inquiryAction(i,"accept",scout,400);assertEquals(before,inquiryRow(i));assertEquals(n,noticesFor(i));assertEquals(0,rows("bookings","venue_id",v));
    }

    @Test void inquiryOverlapUsesEarlierOffset()throws Exception {
        date=LocalDate.of(2026,10,25);clock.now=Instant.parse("2026-10-24T12:00:00Z");UUID v=inquiryVenue();UUID i=open(v,scout);
        offer(i,owner,date,"02:30","03:30",200);
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"),jdbc.queryForObject("select offer_expires_at from availability_inquiries where id=?",OffsetDateTime.class,i).toInstant());
        UUID b=uuid(inquiryAction(i,"accept",scout,200));assertEquals("AWAITING_PAYMENT",state(b));assertEquals(60,detail(b).path("durationMinutes").asInt());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void matchingBoundsAndStableTies(boolean allAvailable)throws Exception {
        String district="QA-EDGE-BATCH-"+UUID.randomUUID();var ids=new ArrayList<UUID>();
        for(int i=0;i<61;i++){UUID v=venue(owner,60);ids.add(v);jdbc.update("update venues set neighbourhood=?,hourly_rate_eur=100,name='QA tie',tags=ARRAY['studio']::text[] where id=?",district,v);if(!allAvailable)jdbc.update("delete from venue_operating_hours where venue_id=?",v);}
        ids.sort(Comparator.comparing(UUID::toString));
        var parsed=json.convertValue(Map.of("neighbourhood",district,"requestedDate",date.toString(),"requestedStartTime","09:00","requestedEndTime","10:00"),ParsedBrief.class);
        var visited=new ArrayList<UUID>();int offset=0;while(true){var batch=filter.findCandidateBatch(parsed,offset);assertTrue(batch.candidates().size()<=30);visited.addAll(batch.candidates().stream().map(com.eema.backend.entity.Venue::getId).toList());if(!batch.hasMore())break;offset=batch.nextOffset();}
        assertEquals(ids,visited);assertEquals(61,new HashSet<>(visited).size());
        clearInvocations(filter);
        when(gemini.generateContent(anyString())).thenReturn("batch-parser","batch-ranker");when(gemini.extractGeneratedText("batch-parser")).thenReturn(json.writeValueAsString(parsed));when(gemini.extractGeneratedText("batch-ranker")).thenReturn("[]");
        req(post("/api/match"),scout,Map.of("brief","QA batch continuation fixture"),200);
        verify(filter,times(1)).findCandidateBatch(parsed,0);
        if(allAvailable){verify(filter,never()).findCandidateBatch(parsed,30);var prompts=org.mockito.ArgumentCaptor.forClass(String.class);verify(gemini,times(2)).generateContent(prompts.capture());String rank=prompts.getAllValues().get(1);for(UUID id:ids.subList(0,30))assertTrue(rank.contains(id.toString()));for(UUID id:ids.subList(30,61))assertFalse(rank.contains(id.toString()));}
        else {verify(filter).findCandidateBatch(parsed,30);verify(filter).findCandidateBatch(parsed,60);verify(gemini,times(1)).generateContent(anyString());}
    }

    static volatile UUID watchedBooking;
    static volatile java.util.concurrent.CountDownLatch declineAtLock;
    @org.springframework.boot.test.context.TestConfiguration static class DeadlineHooks {
        @org.springframework.context.annotation.Bean static org.springframework.beans.factory.config.BeanPostProcessor declineProbe(){return new org.springframework.beans.factory.config.BeanPostProcessor(){public Object postProcessAfterInitialization(Object bean,String name){if(!(bean instanceof com.eema.backend.repository.BookingRepository))return bean;var proxy=new org.springframework.aop.framework.ProxyFactory(bean);proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) call->{if(call.getMethod().getName().equals("findByIdForUpdate")&&Objects.equals(watchedBooking,call.getArguments()[0])&&declineAtLock!=null)declineAtLock.countDown();return call.proceed();});return proxy.getProxy();}};}
    }
    @Autowired org.springframework.transaction.PlatformTransactionManager txManager;
    @RepeatedTest(3) void declineAndExpiryHaveSameTerminalOutcomeInBothOrders()throws Exception {
        for(boolean workerFirst:List.of(false,true)) {
            UUID v=venue(owner,60),b=create(v,scout,"09:00","10:00");clock.now=Instant.parse(detail(b).path("ownerResponseExpiresAt").asText()).plusNanos(1000);
            var held=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var tx=new org.springframework.transaction.support.TransactionTemplate(txManager);
            try {
                var first=pool.submit(()->tx.execute(status->{jdbc.queryForObject("select id from bookings where id=? for update",UUID.class,b);if(workerFirst)bookings.expireDueRequests(OffsetDateTime.now(clock),100);held.countDown();try{assertTrue(release.await(15,java.util.concurrent.TimeUnit.SECONDS));if(!workerFirst)action(b,"decline",owner,409);}catch(Exception e){throw new RuntimeException(e);}return true;}));
                assertTrue(held.await(15,java.util.concurrent.TimeUnit.SECONDS));
                if(workerFirst){watchedBooking=b;declineAtLock=new java.util.concurrent.CountDownLatch(1);var second=pool.submit(()->action(b,"decline",owner,409));assertTrue(declineAtLock.await(15,java.util.concurrent.TimeUnit.SECONDS));release.countDown();first.get(15,java.util.concurrent.TimeUnit.SECONDS);second.get(15,java.util.concurrent.TimeUnit.SECONDS);}
                else {pool.submit(()->bookings.expireDueRequests(OffsetDateTime.now(clock),100)).get(15,java.util.concurrent.TimeUnit.SECONDS);release.countDown();first.get(15,java.util.concurrent.TimeUnit.SECONDS);}
                assertEquals("EXPIRED",state(b));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_EXPIRED"),audit(b));
                assertEquals(1,hooks.committedBookings.stream().filter(e->e.bookingId().equals(b)&&e.type()==com.eema.backend.event.BookingLifecycleEvent.Type.REQUEST_EXPIRED).count());
                assertEquals(0,hooks.committedBookings.stream().filter(e->e.bookingId().equals(b)&&e.type()==com.eema.backend.event.BookingLifecycleEvent.Type.REQUEST_DECLINED).count());
                bookings.expireDueRequests(OffsetDateTime.now(clock),100);action(b,"decline",owner,409);assertEquals(2,audit(b).size());
            } finally {release.countDown();watchedBooking=null;declineAtLock=null;pool.shutdownNow();assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS));}
        }
    }

}
