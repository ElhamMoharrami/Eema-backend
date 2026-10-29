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

/** BK request/commitment contract over real HTTP, service transactions and PostgreSQL. */
@org.springframework.context.annotation.Import(BookingCoreSupport.Config.class)
abstract class BookingCoreSupport extends AuthPostgresSupport {
    @org.springframework.boot.test.context.TestConfiguration
    static class Config {
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor bookingFaults(){
            return new org.springframework.beans.factory.config.BeanPostProcessor(){
                public Object postProcessAfterInitialization(Object bean,String name){
                    if(!(bean instanceof BookingRepository))return bean;
                    var proxy=new org.springframework.aop.framework.ProxyFactory(bean);

                    proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation->{
                        if(invocation.getMethod().getName().equals("findOverlappingRequestedForUpdate") && Objects.equals(FAIL_COMPETITORS,invocation.getArguments()[0]))throw new com.eema.backend.exception.ConflictException("QA injected competitor conflict");
                        return invocation.proceed();
                    });return proxy.getProxy();
                }
            };
        }

        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        TestClock bookingClock(){return new TestClock();}
    }
    static class TestClock extends Clock {
        volatile Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        public Instant instant(){return now;}
    }
    static volatile UUID FAIL_COMPETITORS;
    @AfterEach void disarmFaults(){FAIL_COMPETITORS=null;}
    @Autowired TestClock clock;
    @Autowired VenueRepository venues;
    @Autowired UserRepository users;
    @Autowired BookingService bookings;
    @Autowired BookingPriceCalculator calculator;
    @Autowired BookingWorkflowProperties windows;
    @MockBean MollieProviderClient mollie;
    static JsonNode owner,other,scout,second,admin;
    static final List<UUID> policies=new ArrayList<>();
    LocalDate date;
    @BeforeEach void setup()throws Exception{
        clock.now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);date=LocalDate.now().plusDays(14);
        if(owner!=null)return;
        owner=actor("OWNER");other=actor("OWNER");scout=actor("SCOUT");second=actor("SCOUT");admin=actor("SCOUT");
        jdbc.update("update users set role='ADMIN' where id=?",id(admin));
        for(var a:List.of(owner,other,scout,second))jdbc.update("update users set email_verified=true where id=?",id(a));
        for(var type:PolicyType.values()){
            var p=req(post("/api/admin/policies"),admin,Map.of("policyType",type,"version","QA-BK-"+UUID.randomUUID(),"title","QA TEST CONTENT — NOT LEGAL TERMS","content","QA TEST CONTENT — NOT LEGAL TERMS","effectiveAt",OffsetDateTime.now().toString()),201);
            UUID pid=uuid(p);req(post("/api/admin/policies/"+pid+"/publish"),admin,null,200);policies.add(pid);
        }
        for(var a:List.of(owner,other,scout,second))acceptPolicies(a);
    }
    void acceptPolicies(JsonNode a)throws Exception{req(post("/api/policies/accept"),a,Map.of("policyVersionIds",policies),200);}
    JsonNode req(MockHttpServletRequestBuilder r,JsonNode a,Object body,int expected)throws Exception{
        r.contextPath("/api");if(a!=null)r.header("Authorization","Bearer "+access(a));if(body!=null)r.contentType("application/json").content(json.writeValueAsBytes(body));
        var response=mvc.perform(r).andReturn().getResponse();assertEquals(expected,response.getStatus(),response.getContentAsString());
        return response.getContentAsByteArray().length==0?json.nullNode():json.readTree(response.getContentAsByteArray());
    }
    UUID uuid(JsonNode node){return UUID.fromString(node.path("id").asText());}
    UUID venue(JsonNode owning,int minimum){
        var v=venues.saveAndFlush(Venue.builder().owner(users.findById(id(owning)).orElseThrow()).name("QA Booking Studio").neighbourhood("QA-BK-"+UUID.randomUUID()).status(VenueStatus.ACTIVE).sizeM2(100).hourlyRateEur(new BigDecimal("125.50")).minimumBookingMinutes(minimum).fullDayRateEur(new BigDecimal("900")).minimumLeadTimeMinutes(60).timezone("Europe/Amsterdam").tags(List.of()).photoPaths(List.of()).build());
        for(var day:DayOfWeek.values())jdbc.update("insert into venue_operating_hours(id,venue_id,day_of_week,start_time,end_time) values (?,?,?,?::time,?::time)",UUID.randomUUID(),v.getId(),day.name(),"07:00","20:00");return v.getId();
    }
    Map<String,Object> body(UUID v,String start,String end){return new HashMap<>(Map.of("venueId",v,"shootDate",date.toString(),"startTime",start,"endTime",end,"rawBrief","QA fictional production","scoutContact",Map.of("name","QA Supplied Contact","email","booking-contact@example.test","phone","+31000000000")));}
    UUID create(UUID v,JsonNode a,String start,String end)throws Exception{return uuid(req(post("/api/bookings"),a,body(v,start,end),201));}
    JsonNode detail(UUID b)throws Exception{return req(get("/api/bookings/"+b),scout,null,200);}
    String state(UUID b){return jdbc.queryForObject("select status::text from bookings where id=?",String.class,b);}
    String row(UUID b){return jdbc.queryForObject("select row_to_json(b)::text from bookings b where id=?",String.class,b);}
    List<String> audit(UUID b){return jdbc.queryForList("select event_type::text from booking_events where booking_id=? order by created_at,id",String.class,b);}
    Object references(UUID b){return jdbc.queryForList("select policy_type,policy_version_id from booking_policy_versions where booking_id=? order by policy_type",b);}
    long notices(UUID b){return events.stream(BookingLifecycleEvent.class).filter(e->e.bookingId().equals(b)).count();}
    JsonNode action(UUID b,String action,JsonNode a,int expected)throws Exception{return req(post("/api/bookings/"+b+"/"+action),a,null,expected);}
    JsonNode available(UUID v)throws Exception{return req(get("/api/venues/"+v+"/availability").param("from",date.toString()).param("to",date.toString()),null,null,200);}

}
