package com.eema.backend.integration;

import com.eema.backend.entity.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.eema.backend.event.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Reuses BK's fictional actors, real PostgreSQL and fixed clock, not BK test methods. */
@org.springframework.context.annotation.Import(InquirySupport.Hooks.class)
abstract class InquirySupport extends BookingCoreSupport {
    record Gate(String repository,Set<UUID> ids,CyclicBarrier barrier,AtomicInteger arrivals) {
        Gate(String repository,Set<UUID> ids){this(repository,ids,new CyclicBarrier(2),new AtomicInteger());}
    }
    static volatile Gate gate;
    static volatile UUID awaitingVenue;
    static volatile CountDownLatch venueAttempt;
    @org.springframework.boot.test.context.TestConfiguration
    static class Hooks {
        @org.springframework.context.annotation.Bean static org.springframework.beans.factory.config.BeanPostProcessor inquiryBarriers(){
            return new org.springframework.beans.factory.config.BeanPostProcessor(){
                public Object postProcessAfterInitialization(Object bean,String name){
                    if(!(bean instanceof AvailabilityInquiryRepository)&&!(bean instanceof VenueRepository))return bean;
                    String kind=bean instanceof AvailabilityInquiryRepository?"inquiry":"venue";
                    var proxy=new org.springframework.aop.framework.ProxyFactory(bean);
                    proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation->{
                        Gate current=gate;
                        if(current!=null&&kind.equals(current.repository())&&invocation.getMethod().getName().equals("findByIdForUpdate")&&current.ids().contains(invocation.getArguments()[0])){
                            if(current.arrivals().incrementAndGet()<=2)current.barrier().await(15,TimeUnit.SECONDS);
                        }
                        if(kind.equals("venue")&&invocation.getMethod().getName().equals("findByIdForUpdate")&&Objects.equals(awaitingVenue,invocation.getArguments()[0])&&venueAttempt!=null)venueAttempt.countDown();
                        return invocation.proceed();
                    });return proxy.getProxy();
                }
            };
        }
        final List<AvailabilityInquiryLifecycleEvent> published=new CopyOnWriteArrayList<>();
        final List<AvailabilityInquiryLifecycleEvent> committed=new CopyOnWriteArrayList<>();
        final List<BookingLifecycleEvent> committedBookings=new CopyOnWriteArrayList<>();
        @org.springframework.context.event.EventListener public void capture(AvailabilityInquiryLifecycleEvent e){published.add(e);}
        @org.springframework.transaction.event.TransactionalEventListener public void committed(AvailabilityInquiryLifecycleEvent e){committed.add(e);}
        @org.springframework.transaction.event.TransactionalEventListener public void booking(BookingLifecycleEvent e){committedBookings.add(e);}
    }
    @Autowired Hooks hooks;
    @Autowired AvailabilityInquiryService inquiryService;
    @Autowired com.eema.backend.config.AvailabilityInquiryProperties offerProperties;
    boolean checkoutExercised;
    @AfterEach void clearInquiryHooks(){gate=null;awaitingVenue=null;venueAttempt=null;if(!checkoutExercised)org.mockito.Mockito.verifyNoInteractions(mollie);}
    UUID inquiryVenue(){UUID v=venue(owner,60);jdbc.update("update venue_operating_hours set start_time='09:00',end_time='17:00' where venue_id=?",v);return v;}
    Map<String,Object> inquiryBody(UUID v){return new HashMap<>(Map.of("venueId",v,"requestedDate",date.toString(),"requestedStartTime","18:00","requestedEndTime","20:00","message","QA one-off inquiry"));}
    UUID open(UUID v,JsonNode a)throws Exception{return uuid(req(post("/api/availability-inquiries"),a,inquiryBody(v),201));}
    Map<String,Object> offerBody(LocalDate day,String start,String end){return Map.of("offeredDate",day.toString(),"offeredStartTime",start,"offeredEndTime",end);}
    JsonNode offer(UUID i,JsonNode a,LocalDate day,String start,String end,int code)throws Exception{return req(post("/api/availability-inquiries/"+i+"/offer"),a,offerBody(day,start,end),code);}
    JsonNode inquiryAction(UUID i,String action,JsonNode a,int code)throws Exception{return req(post("/api/availability-inquiries/"+i+"/"+action),a,null,code);}
    String inquiryState(UUID i){return jdbc.queryForObject("select status from availability_inquiries where id=?",String.class,i);}
    String inquiryRow(UUID i){return jdbc.queryForObject("select row_to_json(i)::text from availability_inquiries i where id=?",String.class,i);}
    long noticesFor(UUID i){return hooks.committed.stream().filter(e->e.inquiryId().equals(i)).count();}
    List<String> calendar(UUID v){return List.of(jdbc.queryForObject("select coalesce(json_agg(row_to_json(h) order by h.id)::text,'[]') from venue_operating_hours h where venue_id=?",String.class,v),jdbc.queryForObject("select coalesce(json_agg(row_to_json(o) order by o.id)::text,'[]') from venue_availability_overrides o where venue_id=?",String.class,v));}
    int httpAccept(UUID i,JsonNode a)throws Exception{return mvc.perform(post("/api/availability-inquiries/"+i+"/accept").contextPath("/api").header("Authorization","Bearer "+access(a))).andReturn().getResponse().getStatus();}
}
