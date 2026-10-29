package com.eema.backend.integration;

import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@org.springframework.context.annotation.Import(IncidentIntegrationTest.Faults.class)
class IncidentIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 static volatile UUID raceId;
 static volatile CyclicBarrier barrier;
 static volatile boolean failAudit;
 @org.springframework.boot.test.context.TestConfiguration static class Faults {
  @org.springframework.context.annotation.Bean static org.springframework.beans.factory.config.BeanPostProcessor incidentFaults(){return new org.springframework.beans.factory.config.BeanPostProcessor(){public Object postProcessAfterInitialization(Object bean,String name){
   if(!(bean instanceof IncidentReportRepository)&&!(bean instanceof MarketplaceTrustEventRepository))return bean;
   var proxy=new org.springframework.aop.framework.ProxyFactory(bean);
   proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) call->{
    if(bean instanceof IncidentReportRepository&&call.getMethod().getName().equals("findByIdForUpdate")&&Objects.equals(raceId,call.getArguments()[0]))barrier.await(15,TimeUnit.SECONDS);
    if(bean instanceof MarketplaceTrustEventRepository&&call.getMethod().getName().equals("save")&&failAudit)throw new IllegalStateException("QA incident audit failure");
    return call.proceed();});return proxy.getProxy();
  }};}
 }
 @AfterEach void reset(){raceId=null;barrier=null;failAudit=false;verifyNoInteractions(mollie);}
 UUID booking()throws Exception{return create(venue(owner,60),scout,"09:00","10:00");}
 String path(UUID b){return "/api/bookings/"+b+"/incidents";}
 String adminPath(UUID i){return "/api/admin/incidents/"+i;}
 JsonNode report(UUID b,JsonNode actor,String category,String text,int expected)throws Exception{return req(post(path(b)),actor,Map.of("category",category,"description",text),expected);}
 UUID incident(UUID b)throws Exception{return uuid(report(b,scout,"OTHER","QA incident",201));}
 JsonNode act(UUID i,String action,int expected)throws Exception{return req(post(adminPath(i)+"/"+action),admin,action.equals("resolve")?Map.of("resolutionNote","QA internal resolution"):null,expected);}
 String incidentRow(UUID i){return jdbc.queryForObject("select row_to_json(i)::text from incident_reports i where id=?",String.class,i);}
 long audits(UUID i){return rows("marketplace_trust_events","subject_id",i);}
 Object isolated(UUID b){return List.of(row(b),jdbc.queryForList("select row_to_json(v)::text from venues v where id=(select venue_id from bookings where id=?)",b),jdbc.queryForList("select row_to_json(p)::text from owner_payouts p where booking_id=?",b),jdbc.queryForList("select row_to_json(l)::text from owner_financial_liabilities l where booking_id=?",b),audit(b),references(b));}
 @ParameterizedTest @EnumSource(IncidentCategory.class)
 void allCategoriesAllParticipants(IncidentCategory category)throws Exception{
  UUID b=booking();Object before=isolated(b);long notices=notices(b);
  for(JsonNode actor:List.of(scout,owner,admin)){var result=report(b,actor,category.name(),"  QA José 李雷\nplain & text  ",201);UUID i=uuid(result);assertEquals("OPEN",result.path("status").asText());assertEquals(id(actor).toString(),result.path("reportedByUserId").asText());assertEquals("QA José 李雷\nplain & text",result.path("description").asText());assertTrue(result.path("resolutionNote").isNull());assertEquals(1,audits(i));assertEquals("INCIDENT_REPORTED",jdbc.queryForObject("select event_type from marketplace_trust_events where subject_id=?",String.class,i));}
  for(JsonNode actor:List.of(scout,owner,admin))assertEquals(3,req(get(path(b)),actor,null,200).size());assertEquals(before,isolated(b));assertEquals(notices,notices(b));
 }
 @Test void participantDenialAndMissingIdsLeaveNoWrites()throws Exception{
  UUID b=booking();Object before=isolated(b);for(JsonNode actor:List.of(second,other)){report(b,actor,"OTHER","denied",403);req(get(path(b)),actor,null,403);}report(b,null,"OTHER","denied",401);req(get(path(b)),null,null,401);report(UUID.randomUUID(),scout,"OTHER","missing",404);req(get(path(UUID.randomUUID())),scout,null,404);assertEquals(0,rows("incident_reports","booking_id",b));assertEquals(before,isolated(b));
 }
 @ParameterizedTest @ValueSource(strings={""," ","\n\t","<script>x</script>","a > b","a < b"})
 void invalidDescriptions(String text)throws Exception{UUID b=booking();report(b,scout,"OTHER",text,400);assertEquals(0,rows("incident_reports","booking_id",b));}
 @Test void exactLengthsUnknownCategoryAndMissingFields()throws Exception{
  UUID b=booking();report(b,scout,"OTHER","x".repeat(4000),201);report(b,scout,"OTHER","x".repeat(4001),400);report(b,scout,"UNKNOWN","valid",400);req(post(path(b)),scout,Map.of("description","valid"),400);req(post(path(b)),scout,Map.of("category","OTHER"),400);assertEquals(1,rows("incident_reports","booking_id",b));
 }
 @ParameterizedTest @EnumSource(BookingStatus.class)
 void everyBookingStateAllowsSupportIntake(BookingStatus status)throws Exception{UUID b=booking();jdbc.update("update bookings set status=?::booking_status where id=?",status.name(),b);Object before=isolated(b);incident(b);assertEquals(before,isolated(b));}
 @Test void noVerificationOrPolicyPrerequisiteButInactiveAccountDenied()throws Exception{
  UUID b=booking();jdbc.update("update users set email_verified=false where id=?",id(scout));jdbc.update("delete from policy_acceptances where user_id=?",id(scout));try{incident(b);req(get(path(b)),scout,null,200);jdbc.update("update users set active=false where id=?",id(scout));report(b,scout,"OTHER","denied",401);}finally{jdbc.update("update users set active=true,email_verified=true where id=?",id(scout));acceptPolicies(scout);}
 }
 @Test void adminEndpointsRejectAllOtherActors()throws Exception{
  UUID b=booking(),i=incident(b);String before=incidentRow(i);for(JsonNode actor:Arrays.asList(scout,owner,second,other,null)){int status=actor==null?401:403;req(get("/api/admin/incidents"),actor,null,status);req(get(adminPath(i)),actor,null,status);for(String action:List.of("start-review","resolve","close"))req(post(adminPath(i)+"/"+action),actor,action.equals("resolve")?Map.of("resolutionNote","QA note"):null,status);}assertEquals(before,incidentRow(i));assertEquals(1,audits(i));
 }
 @Test void lifecycleAuditPrivacyAndFinancialIsolation()throws Exception{
  UUID b=booking(),i=incident(b);
  jdbc.update("insert into owner_payouts(id,booking_id,owner_id,venue_id,gross_retained_amount_eur,commission_eur,provider_cost_eur,payout_amount_eur,status,eligible_at) select ?,id,?,venue_id,125.50,25.10,1.50,98.90,'CALCULATED',now() from bookings where id=?",UUID.randomUUID(),id(owner),b);
  jdbc.update("insert into owner_financial_liabilities(id,owner_id,booking_id,type,amount_eur,status) values (?,?,?,'PROVIDER_COST',1.50,'OPEN')",UUID.randomUUID(),id(owner),b);
  Object before=isolated(b);long notices=notices(b);act(i,"start-review",200);var result=act(i,"resolve",200);assertFalse(result.path("resolvedAt").isNull());assertEquals("QA internal resolution",result.path("resolutionNote").asText());act(i,"close",200);
  for(JsonNode actor:List.of(scout,owner,admin)){String participant=req(get(path(b)),actor,null,200).toString();assertFalse(participant.contains("QA internal resolution"));}
  assertEquals("QA internal resolution",req(get(adminPath(i)),admin,null,200).path("resolutionNote").asText());assertEquals(4,audits(i));assertEquals(0,jdbc.queryForObject("select count(*) from marketplace_trust_events where subject_id=? and note is not null",Integer.class,i));assertEquals(before,isolated(b));assertEquals(notices,notices(b));
 }
 @ParameterizedTest @EnumSource(IncidentStatus.class)
 void allIllegalTransitionsLeaveRecordAndAuditUnchanged(IncidentStatus state)throws Exception{
  UUID i=incident(booking());if(state.ordinal()>=1)act(i,"start-review",200);if(state.ordinal()>=2)act(i,"resolve",200);if(state.ordinal()>=3)act(i,"close",200);
  for(String action:List.of("start-review","resolve","close")){boolean legal=state==IncidentStatus.OPEN&&action.equals("start-review")||state==IncidentStatus.UNDER_REVIEW&&action.equals("resolve")||state==IncidentStatus.RESOLVED&&action.equals("close");if(legal)continue;String before=incidentRow(i);long count=audits(i);act(i,action,409);assertEquals(before,incidentRow(i));assertEquals(count,audits(i));}
 }
 @ParameterizedTest @ValueSource(strings={""," \n\t","<b>note</b>","a > b"})
 void resolutionValidationAtomic(String note)throws Exception{UUID i=incident(booking());act(i,"start-review",200);String before=incidentRow(i);req(post(adminPath(i)+"/resolve"),admin,Map.of("resolutionNote",note),400);assertEquals(before,incidentRow(i));assertEquals(2,audits(i));}
 @Test void noteLengthsAndPlainText()throws Exception{UUID i=incident(booking());act(i,"start-review",200);req(post(adminPath(i)+"/resolve"),admin,Map.of("resolutionNote","x".repeat(2001)),400);req(post(adminPath(i)+"/resolve"),admin,Map.of(),400);String text="李雷\n"+"x".repeat(1997);assertEquals(text,req(post(adminPath(i)+"/resolve"),admin,Map.of("resolutionNote",text),200).path("resolutionNote").asText());}
 @Test void adminFiltersMissingRecordsAndBoundedPagination()throws Exception{
  UUID b=booking(),i=incident(b);var list=req(get("/api/admin/incidents").param("bookingId",b.toString()).param("status","OPEN").param("category","OTHER").param("size","9999"),admin,null,200);assertEquals(1,list.path("totalElements").asInt());assertTrue(list.path("size").asInt()<=100);assertEquals(i.toString(),list.path("content").get(0).path("id").asText());act(i,"start-review",200);assertEquals(0,req(get("/api/admin/incidents").param("bookingId",b.toString()).param("status","OPEN"),admin,null,200).path("totalElements").asInt());req(get(adminPath(UUID.randomUUID())),admin,null,404);for(String a:List.of("start-review","resolve","close"))act(UUID.randomUUID(),a,404);
 }
 @Test void auditFailureRollsBackCreationAndTransition()throws Exception{
  UUID b=booking();failAudit=true;try{report(b,scout,"OTHER","QA audit failure",500);}finally{failAudit=false;}assertEquals(0,rows("incident_reports","booking_id",b));UUID i=incident(b);String before=incidentRow(i);failAudit=true;try{act(i,"start-review",500);}finally{failAudit=false;}assertEquals(before,incidentRow(i));assertEquals(1,audits(i));
 }
 @Test void databaseRejectsBrokenReferencesAndResolutionShape()throws Exception{
  UUID b=booking(),i=incident(b);String before=incidentRow(i);
  for(String change:List.of("booking_id='"+UUID.randomUUID()+"'","reported_by_user_id='"+UUID.randomUUID()+"'","category='UNKNOWN'","status='UNKNOWN'","description='<markup>'","description=''","status='RESOLVED'","admin_resolution_note='premature'")){
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update incident_reports set "+change+" where id=?",i));assertEquals(before,incidentRow(i));
  }
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("delete from bookings where id=?",b));assertEquals(before,incidentRow(i));
 }
 @RepeatedTest(3) void competingTransitionsHaveOneWinner()throws Exception{
  UUID i=incident(booking());for(String action:List.of("start-review","resolve","close")){raceId=i;barrier=new CyclicBarrier(2);long before=audits(i);try(var pool=Executors.newFixedThreadPool(2)){Callable<Integer> call=()->{var request=post(adminPath(i)+"/"+action).contextPath("/api").header("Authorization","Bearer "+access(admin));if(action.equals("resolve"))request.contentType("application/json").content("{\"resolutionNote\":\"QA concurrent resolution\"}");return mvc.perform(request).andReturn().getResponse().getStatus();};var a=pool.submit(call);var b=pool.submit(call);assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get(25,TimeUnit.SECONDS),b.get(25,TimeUnit.SECONDS)).sorted().toList());}finally{raceId=null;barrier=null;}assertEquals(before+1,audits(i));}assertEquals("CLOSED",req(get(adminPath(i)),admin,null,200).path("status").asText());
 }
}
