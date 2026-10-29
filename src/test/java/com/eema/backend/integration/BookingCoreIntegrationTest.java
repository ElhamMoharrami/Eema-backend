package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.config.BookingWorkflowProperties;
import com.eema.backend.event.BookingLifecycleEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class BookingCoreIntegrationTest extends BookingCoreSupport {
    @Autowired PaymentCreationCoordinator checkoutClaims;
    @BeforeAll static void freshCoreAccounts(){owner=null;policies.clear();}
    @ParameterizedTest @ValueSource(ints={60,360})
    void exact270MinutesAndHistoricalSnapshotsSurviveAcceptance(int minimum)throws Exception{
        UUID v=venue(owner,minimum),b=create(v,scout,"09:15","13:45");var before=detail(b);var refs=references(b);
        var quote=calculator.calculate(new BigDecimal("125.50"),minimum,new BigDecimal("900"),LocalTime.of(9,15),LocalTime.of(13,45));
        assertEquals(270,before.path("durationMinutes").asInt());assertEquals(quote.billableMinutes(),before.path("billableMinutes").asInt());assertEquals(0,quote.finalPriceEur().compareTo(before.path("venuePriceEur").decimalValue()));
        assertEquals("REQUESTED",state(b));assertEquals(4,rows("booking_policy_versions","booking_id",b));assertFalse(refs.toString().contains("PRIVACY"));
        assertTrue(before.path("molliePaymentId").isNull());var open=available(v);
        jdbc.update("update venues set hourly_rate_eur=999,minimum_booking_minutes=600,full_day_rate_eur=9999 where id=?",v);
        jdbc.update("update users set full_name='QA Changed Live',phone='+31000000001' where id in (?,?)",id(scout),id(owner));
        var accepted=action(b,"accept",owner,200);
        for(String field:List.of("venuePriceEur","hourlyRateEurSnapshot","minimumBookingMinutesSnapshot","fullDayRateEurSnapshot","durationMinutes","billableMinutes","commissionPercentage","commissionEur","scoutContact","ownerContact"))assertEquals(before.get(field),accepted.get(field),field);
        assertEquals(0,app.commission().percentage().compareTo(before.path("commissionPercentage").decimalValue()));
        assertEquals(0,quote.finalPriceEur().compareTo(checkoutClaims.claim(b,id(scout)).amount()));
        assertEquals(refs,references(b));assertNotEquals(open,available(v));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_ACCEPTED"),audit(b));verifyNoInteractions(mollie);
    }
    @ParameterizedTest @ValueSource(strings={"equal","reverse","past","outside","brief","contact","inactive","email","policy","owner","admin","anonymous"})
    void rejectedCreationHasNoRowsEventsOrProviderEffects(String kind)throws Exception{
        UUID v=venue(owner,60);var b=body(v,"09:00","12:00");JsonNode a=scout;int code=400;
        switch(kind){
            case "equal"->b.put("endTime","09:00");case "reverse"->b.put("endTime","08:00");
            case "past"->{b.put("shootDate",LocalDate.now().minusDays(1).toString());code=409;}
            case "outside"->{b.put("startTime","05:00");b.put("endTime","06:00");code=409;}
            case "brief"->b.put("rawBrief"," ");case "contact"->b.put("scoutContact",null);
            case "inactive"->{jdbc.update("update venues set status='SUSPENDED' where id=?",v);code=404;}
            case "email"->{a=actor("SCOUT");code=403;}
            case "policy"->{a=actor("SCOUT");jdbc.update("update users set email_verified=true where id=?",id(a));code=403;}
            case "owner"->{a=owner;code=403;}case "admin"->{a=admin;code=403;}case "anonymous"->{a=null;code=401;}
        }
        long count=events.stream(BookingLifecycleEvent.class).count();req(post("/api/bookings"),a,b,code);
        assertEquals(0,rows("bookings","venue_id",v));assertEquals(count,events.stream(BookingLifecycleEvent.class).count());verifyNoInteractions(mollie);
    }
    @Test void halfOpenCompetitorsOnlyBecomeUnavailable()throws Exception{
        UUID v=venue(owner,60);var initial=available(v);UUID winner=create(v,scout,"09:00","12:00");
        var losers=new ArrayList<UUID>();for(String[] r:List.of(new String[]{"09:00","12:00"},new String[]{"11:59","13:00"},new String[]{"10:00","11:00"}))losers.add(create(v,second,r[0],r[1]));
        UUID before=create(v,second,"08:00","09:00"),after=create(v,second,"12:00","15:00");
        var different=body(v,"09:00","12:00");different.put("shootDate",date.plusDays(1).toString());UUID otherDay=uuid(req(post("/api/bookings"),second,different,201));
        UUID terminal=create(v,second,"09:00","12:00");action(terminal,"decline",owner,200);String terminalRow=row(terminal);assertEquals(initial,available(v));
        var accepted=action(winner,"accept",owner,200);assertEquals(clock.instant().plus(Duration.ofHours(windows.paymentWindowHours())),OffsetDateTime.parse(accepted.path("paymentExpiresAt").asText()).toInstant());
        for(UUID b:losers){assertEquals("UNAVAILABLE",state(b));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_BECAME_UNAVAILABLE"),audit(b));assertEquals(2,notices(b));}
        assertEquals(terminalRow,row(terminal));for(UUID b:List.of(before,after,otherDay))assertEquals("REQUESTED",state(b));assertEquals("AWAITING_PAYMENT",state(winner));assertTrue(accepted.path("molliePaymentId").isNull());verifyNoInteractions(mollie);
    }
    @ParameterizedTest @ValueSource(longs={-1,0,1})
    void exactResponseDeadlineCommitsExpiryBeforeConflict(long microseconds)throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","12:00");var deadline=clock.instant().plus(Duration.ofHours(windows.ownerResponseWindowHours()));assertEquals(deadline,OffsetDateTime.parse(detail(b).path("ownerResponseExpiresAt").asText()).toInstant());
        clock.now=deadline.plusNanos(microseconds*1000);action(b,"accept",owner,microseconds<0?200:409);
        assertEquals(microseconds<0?"AWAITING_PAYMENT":"EXPIRED",state(b));assertEquals(microseconds<0?"REQUEST_ACCEPTED":"REQUEST_EXPIRED",audit(b).get(1));
        if(microseconds>=0){assertEquals(2,notices(b));action(b,"accept",owner,409);assertEquals(2,audit(b).size());}
    }
    @RepeatedTest(3) void persistedExpiryIsIdempotent()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","12:00");var now=OffsetDateTime.parse(detail(b).path("ownerResponseExpiresAt").asText());clock.now=now.toInstant();bookings.expireDueRequests(now,1000);bookings.expireDueRequests(now,1000);
        assertEquals("EXPIRED",state(b));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_EXPIRED"),audit(b));assertEquals(2,notices(b));
    }
    @ParameterizedTest @EnumSource(value=BookingStatus.class,names="REQUESTED",mode=EnumSource.Mode.EXCLUDE)
    void declineRejectsEveryNonRequestStateWithoutEffects(BookingStatus status)throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","12:00"); // Seed only resulting states; settlement is outside BK.
        jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);String before=row(b);long n=notices(b);action(b,"decline",owner,409);assertEquals(before,row(b));assertEquals(1,audit(b).size());assertEquals(n,notices(b));
    }
    @ParameterizedTest @ValueSource(strings={"accept","decline"})
    void actorBoundaryAndExplicitAdminDelegation(String operation)throws Exception{
        UUID v=venue(owner,60),b=create(v,scout,"09:00","12:00");String before=row(b);
        for(var a:List.of(other,scout,second)){action(b,operation,a,403);assertEquals(before,row(b));}
        action(b,operation,null,401);assertEquals(before,row(b));action(b,operation,admin,200);assertEquals(operation.equals("accept")?"AWAITING_PAYMENT":"DECLINED",state(b));action(b,operation,admin,409);assertEquals(2,audit(b).size());assertEquals("ADMIN",jdbc.queryForObject("select actor_type::text from booking_events where booking_id=? and event_type<>'REQUEST_SUBMITTED'",String.class,b));verifyNoInteractions(mollie);
    }
    @Test void ordinaryReadListsAreScopedAndDetailsDoNotSerializeUserSecrets()throws Exception{
        UUID a=create(venue(owner,60),scout,"09:00","12:00"),b=create(venue(owner,60),second,"09:00","12:00"),c=create(venue(other,60),scout,"09:00","12:00");
        var fixtures=Set.of(a,b,c);var expected=Map.of(scout,Set.of(a,c),second,Set.of(b),owner,Set.of(a,b),other,Set.of(c),admin,fixtures);
        for(var e:expected.entrySet()){
            var list=req(get("/api/bookings"),e.getKey(),null,200);Set<UUID> found=new HashSet<>();list.forEach(n->{UUID id=uuid(n);if(fixtures.contains(id))found.add(id);});assertEquals(e.getValue(),found);
            for(UUID id:fixtures)req(get("/api/bookings/"+id),e.getKey(),null,e.getValue().contains(id)?200:403);
        }
        req(get("/api/bookings"),null,null,401);req(get("/api/bookings/"+a),null,null,401);req(get("/api/bookings/"+UUID.randomUUID()),scout,null,404);req(get("/api/bookings/not-a-uuid"),scout,null,400);
        String serialized=detail(a).toString().toLowerCase();for(String secret:List.of("password","refreshtoken","accesstoken","iban","encrypted","reset_token","verification_token"))assertFalse(serialized.contains(secret),secret);
    }

    @Test void policyReplacementAndLiveEmailsDoNotRewriteBookingHistory()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","12:00");var before=detail(b);var historical=references(b);
        var replacement=req(post("/api/admin/policies"),admin,Map.of("policyType","TERMS_OF_SERVICE","version","QA-BK-"+UUID.randomUUID(),"title","QA TEST CONTENT — NOT LEGAL TERMS","content","QA TEST CONTENT — NOT LEGAL TERMS replacement","effectiveAt",OffsetDateTime.now().toString()),201);
        UUID pid=uuid(replacement);req(post("/api/admin/policies/"+pid+"/publish"),admin,null,200);
        // Resolve the required IDs from persistence; acceptance itself stays the real HTTP contract.
        policies.clear();policies.addAll(jdbc.queryForList("select id from policy_versions where status='PUBLISHED'",UUID.class));
        action(b,"accept",owner,403);action(b,"decline",owner,403);assertEquals("REQUESTED",state(b));assertEquals(historical,references(b));
        for(var a:List.of(owner,other,scout,second))acceptPolicies(a);
        // QA internal profile fixture: tokens identify IDs; these email changes do not impersonate another account.
        String oldScout=email(scout),oldOwner=email(owner);
        try{
            jdbc.update("update users set email=? where id=?","changed-"+UUID.randomUUID()+"@example.test",id(scout));jdbc.update("update users set email=? where id=?","changed-"+UUID.randomUUID()+"@example.test",id(owner));
            var accepted=action(b,"accept",admin,200);assertEquals(before.get("scoutContact"),accepted.get("scoutContact"));assertEquals(before.get("ownerContact"),accepted.get("ownerContact"));assertEquals(historical,references(b));
        }finally{jdbc.update("update users set email=? where id=?",oldScout,id(scout));jdbc.update("update users set email=? where id=?",oldOwner,id(owner));}
    }
    @Test void competitorFailureRollsBackFlushedWinnerAndAudit()throws Exception{
        UUID v=venue(owner,60),a=create(v,scout,"09:00","12:00"),b=create(v,second,"10:00","11:00");String original=row(a);var open=available(v);FAIL_COMPETITORS=v;
        action(a,"accept",owner,409);assertEquals(original,row(a));assertEquals("REQUESTED",state(b));assertEquals(List.of("REQUEST_SUBMITTED"),audit(a));assertEquals(List.of("REQUEST_SUBMITTED"),audit(b));assertEquals(open,available(v));assertEquals(1,notices(a));
    }
    @Test void reservationAndNotificationIntentCommitTogetherWithoutProvider()throws Exception{
        UUID b=create(venue(owner,60),scout,"09:00","12:00");action(b,"accept",owner,200);
        assertEquals("AWAITING_PAYMENT",state(b));assertEquals(List.of("REQUEST_SUBMITTED","REQUEST_ACCEPTED"),audit(b));
        assertEquals(1,jdbc.queryForObject("select count(*) from notification_outbox where aggregate_id=? and notification_type='REQUEST_ACCEPTED' and status='PENDING'",Integer.class,b));
        // Provider-failure persistence is exercised by OperationsNotificationIntegrationTest.
    }
    @ParameterizedTest @ValueSource(strings={"BLOCKED","SUSPENDED","LEAD"})
    void acceptanceRevalidatesCurrentConditions(String condition)throws Exception{
        UUID v=venue(owner,60),b=create(v,scout,"09:00","12:00");
        switch(condition){
            case "BLOCKED"->req(post("/api/venues/"+v+"/availability/overrides"),owner,Map.of("date",date.toString(),"type","BLOCKED","allDay",true),201);
            case "SUSPENDED"->jdbc.update("update venues set status='SUSPENDED' where id=?",v);
            case "LEAD"->{clock.now=date.atTime(8,30).atZone(ZoneId.of("Europe/Amsterdam")).toInstant();jdbc.update("update bookings set owner_response_expires_at=? where id=?",OffsetDateTime.ofInstant(clock.now.plusSeconds(86400),ZoneOffset.UTC),b);}
        }
        action(b,"accept",owner,409);assertEquals(condition.equals("LEAD")?"EXPIRED":"REQUESTED",state(b));assertEquals(condition.equals("LEAD")?2:1,audit(b).size());verifyNoInteractions(mollie);
        if(condition.equals("LEAD"))assertTrue(jdbc.queryForObject("select payload::text from booking_events where booking_id=? and event_type='REQUEST_EXPIRED'",String.class,b).contains("MINIMUM_LEAD_TIME_NOT_MET"));
    }
}
