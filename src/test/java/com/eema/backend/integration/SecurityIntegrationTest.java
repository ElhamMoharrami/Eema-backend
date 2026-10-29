package com.eema.backend.integration;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.GlobalExceptionHandler;
import com.eema.backend.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties="app.security.hsts-enabled=true")
@Import(SecurityIntegrationTest.Faults.class)
class SecurityIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @AfterEach void noProvider(){verifyNoInteractions(mollie);}
 @org.springframework.boot.test.context.TestConfiguration static class Faults {
  @Bean FailureEndpoint failureEndpoint(){return new FailureEndpoint();}
 }
 @RestController static class FailureEndpoint {
  @GetMapping("/qa/security/failure") String fail(){throw new RuntimeException("QA wrapper",new IllegalStateException("QA_SECRET_HTTP_MARKER\r\nFORGED"));}
 }
 MockHttpServletResponse response(MockHttpServletRequestBuilder r,JsonNode actor)throws Exception{
  r.contextPath("/api");if(actor!=null)r.header("Authorization","Bearer "+access(actor));return mvc.perform(r).andReturn().getResponse();
 }
 @Test void unexpectedHttpFailureIsSafeInBodyAndLogs()throws Exception{
  Logger logger=(Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);var log=new ListAppender<ILoggingEvent>();log.start();logger.addAppender(log);
  try{var r=response(get("/api/qa/security/failure"),scout);assertEquals(500,r.getStatus());assertFalse(r.getContentAsString().contains("QA_SECRET_HTTP_MARKER"));assertFalse(log.list.isEmpty());for(var e:log.list){assertFalse(e.getFormattedMessage().contains("QA_SECRET_HTTP_MARKER"));assertNull(e.getThrowableProxy());}}
  finally{logger.detachAppender(log);log.stop();}
 }
 @Test void malformedJwtThroughHttpDoesNotLogHeaderContents()throws Exception{
  Logger logger=(Logger)LoggerFactory.getLogger(JwtAuthenticationFilter.class);Level previous=logger.getLevel();logger.setLevel(Level.DEBUG);var log=new ListAppender<ILoggingEvent>();log.start();logger.addAppender(log);
  String token=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"secret\":\"QA_SECRET_HTTP_MARKER\",BROKEN}".getBytes())+".e30.c2ln";
  try{var r=response(get("/api/auth/me").header("Authorization","Bearer "+token),null);assertEquals(401,r.getStatus());assertFalse(r.getContentAsString().contains("QA_SECRET_HTTP_MARKER"));assertFalse(log.list.isEmpty());for(var e:log.list){assertFalse(e.getFormattedMessage().contains("QA_SECRET_HTTP_MARKER"));assertNull(e.getThrowableProxy());}}
  finally{logger.setLevel(previous);logger.detachAppender(log);log.stop();}
 }
 @Test void corsAllowsOnlyConfiguredOriginsMethodsAndHeaders()throws Exception{
  var r=response(options("/api/bookings").header("Origin","http://localhost:3000").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","Authorization,Content-Type,X-Requested-With,X-Request-ID"),null);
  assertEquals(200,r.getStatus());assertEquals("http://localhost:3000",r.getHeader("Access-Control-Allow-Origin"));assertEquals("true",r.getHeader("Access-Control-Allow-Credentials"));assertTrue(r.getHeader("Access-Control-Allow-Headers").contains("X-Request-ID"));
  for(String origin:List.of("https://unlisted.example","null","*","http://localhost.evil.example:3000")){var denied=response(options("/api/bookings").header("Origin",origin).header("Access-Control-Request-Method","POST"),null);assertEquals(403,denied.getStatus());assertNull(denied.getHeader("Access-Control-Allow-Origin"));}
  for(var pair:Map.of("Access-Control-Request-Method","TRACE","Access-Control-Request-Headers","X-Secret-Unknown").entrySet()){var q=options("/api/bookings").header("Origin","http://localhost:3000").header("Access-Control-Request-Method","POST");if(pair.getKey().endsWith("Method"))q=options("/api/bookings").header("Origin","http://localhost:3000");q.header(pair.getKey(),pair.getValue());assertEquals(403,response(q,null).getStatus());}
  var actual=response(get("/api/auth/me").header("Origin","http://localhost:3000"),scout);assertEquals(200,actual.getStatus());assertTrue(actual.getHeader("Access-Control-Expose-Headers").contains("Content-Disposition"));assertTrue(actual.getHeader("Access-Control-Expose-Headers").contains("X-Request-ID"));
 }
 @Test void securityHeadersAndOptInHstsRespectTransport()throws Exception{
  for(boolean secure:List.of(false,true)){var r=response(get("/api/actuator/health").secure(secure),null);assertEquals(200,r.getStatus());assertEquals("nosniff",r.getHeader("X-Content-Type-Options"));assertEquals("DENY",r.getHeader("X-Frame-Options"));assertEquals("no-referrer",r.getHeader("Referrer-Policy"));assertEquals("default-src 'none'; frame-ancestors 'none'",r.getHeader("Content-Security-Policy"));assertTrue(r.getHeader("Cache-Control").contains("no-store"));if(secure)assertTrue(r.getHeader("Strict-Transport-Security").contains("max-age=31536000"));else assertNull(r.getHeader("Strict-Transport-Security"));}
 }
 @ParameterizedTest @ValueSource(strings={"safe_ID-123.test","bad value","bad\r\nFORGED","🧪",""})
 void correlationIdIsSafeAndDoesNotBleedBetweenRequests(String input)throws Exception{
  var r=response(get("/api/actuator/health").header("X-Request-ID",input),null);assertEquals(input.contains("\r")?400:200,r.getStatus());String id=r.getHeader("X-Request-ID");assertNotNull(id);if(input.equals("safe_ID-123.test"))assertEquals(input,id);else assertTrue(id.matches("[0-9a-f-]{36}"));assertNull(org.slf4j.MDC.get("requestId"));
  String next=response(get("/api/actuator/health").header("X-Request-ID","x".repeat(101)),null).getHeader("X-Request-ID");assertTrue(next.matches("[0-9a-f-]{36}"));assertNotEquals(id,next);
 }
 @Test void actuatorAndFrameworkExposureIsRoleBounded()throws Exception{
  for(var a:Arrays.asList(null,scout,owner,admin)){
   for(String p:List.of("/actuator/health","/actuator/health/readiness","/actuator/health/liveness")){var r=response(get("/api"+p),a);assertEquals(200,r.getStatus());var body=json.readTree(r.getContentAsByteArray());assertTrue(Set.of("status","groups").containsAll(json.convertValue(body,Map.class).keySet()));assertFalse(body.has("components"));assertFalse(body.has("details"));}
   for(String p:List.of("/actuator/metrics","/actuator/metrics/jvm.memory.used"))assertEquals(a==null?401:a==admin?200:403,response(get("/api"+p),a).getStatus());
   for(String p:List.of("/actuator/env","/actuator/configprops","/actuator/heapdump"))assertEquals(a==null?401:404,response(get("/api"+p),a).getStatus());
   for(String p:List.of("/v3/api-docs","/swagger-ui/index.html"))assertEquals(a==null?401:200,response(get("/api"+p),a).getStatus());
  }
  assertEquals(405,response(delete("/api/admin/operations/exceptions"),admin).getStatus());
  assertEquals(403,response(delete("/api/admin/operations/exceptions"),scout).getStatus());
  var metric=json.readTree(response(get("/api/actuator/metrics/eema.workflow.refund.stuck"),admin).getContentAsByteArray());assertEquals(0,metric.path("availableTags").size());
 }
 @Test void databaseRoleActiveAndVerificationWinOverCachedClaims()throws Exception{
  String misleading=jwt.generateAccessToken(User.builder().id(id(scout)).email(email(scout)).role(UserRole.ADMIN).build());
  assertEquals(403,response(get("/api/admin/operations/exceptions").header("Authorization","Bearer "+misleading),null).getStatus());
  jdbc.update("update users set role='SCOUT' where id=?",id(admin));try{assertEquals(403,response(get("/api/admin/operations/exceptions"),admin).getStatus());}finally{jdbc.update("update users set role='ADMIN' where id=?",id(admin));}
  jdbc.update("update users set active=false where id=?",id(scout));try{assertEquals(401,response(get("/api/auth/me"),scout).getStatus());}finally{jdbc.update("update users set active=true where id=?",id(scout));}
  jdbc.update("update users set email_verified=false where id=?",id(scout));try{long before=jdbc.queryForObject("select count(*) from bookings",Long.class);var r=response(post("/api/bookings").header("X-Email-Verified","true").contentType("application/json").content(json.writeValueAsBytes(body(venue(owner,60),"09:00","10:00"))),scout);assertEquals(403,r.getStatus());assertEquals(before,jdbc.queryForObject("select count(*) from bookings",Long.class));}finally{jdbc.update("update users set email_verified=true where id=?",id(scout));}
 }
 @ParameterizedTest @ValueSource(strings={"<script>alert(1)</script>","<img src=x onerror=alert(1)>","Robert'); DROP TABLE users;--","Zoë 李雷 🎬","QA\r\nFORGED","Quotes \" and \\ backslash"})
 void plainTextPolicyAndNameInputsRemainData(String text)throws Exception{
  long usersBefore=jdbc.queryForObject("select count(*) from users",Long.class);
  var a=call("register",Map.of("email",address(),"password",QA_PASSWORD,"fullName",text,"role","SCOUT"),201);assertEquals(text.trim(),a.path("user").path("fullName").asText());assertEquals(usersBefore+1,jdbc.queryForObject("select count(*) from users",Long.class));
  var policy=req(post("/api/admin/policies"),admin,Map.of("policyType","TERMS_OF_SERVICE","version","SEC-"+UUID.randomUUID(),"title",text,"content",text,"effectiveAt",OffsetDateTime.now().toString()),201);assertEquals(text,policy.path("content").asText());
  create(venue(owner,60),scout,"09:00","10:00"); // Ensure an injected match-all query could not pass vacuously.
  var filter=req(get("/api/admin/bookings").param("molliePaymentId",text),admin,null,200);assertTrue(filter.path("empty").asBoolean());
 }
 @Test void dtoSpecificLargeInputBoundsAndSafeErrors()throws Exception{
  long before=jdbc.queryForObject("select count(*) from users",Long.class);call("register",Map.of("email",address(),"password",QA_PASSWORD,"fullName","x".repeat(10000),"role","SCOUT"),400);assertEquals(before,jdbc.queryForObject("select count(*) from users",Long.class));
  var policy=req(post("/api/admin/policies"),admin,Map.of("policyType","TERMS_OF_SERVICE","version","SEC-"+UUID.randomUUID(),"title","QA bounded content","content","x".repeat(10000),"effectiveAt",OffsetDateTime.now().toString()),201);assertEquals(10000,policy.path("content").asText().length());
  var malformed=response(post("/api/auth/login").contentType("application/json").content("{\"password\":\"QA_SECRET_PARSE_MARKER\",BROKEN}"),null);assertEquals(400,malformed.getStatus());assertFalse(malformed.getContentAsString().contains("QA_SECRET_PARSE_MARKER"));
 }
 @Test void authSecretsRemainOutOfNormalDtosAndApplicationLogs()throws Exception{
  Logger logger=(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);var captured=new ListAppender<ILoggingEvent>();captured.start();logger.addAppender(captured);
  try{
   String password="QA_SECRET_PASSWORD_MARKER";
   var a=call("register",Map.of("email",address(),"password",password,"fullName","QA Secret Audit","role","SCOUT"),201);
   String verification=verifier(id(a));call("forgot-password",Map.of("email",email(a)),200);String reset=resetter(id(a));
   var normal=response(get("/api/auth/me"),a);assertEquals(200,normal.getStatus());String dto=normal.getContentAsString();
   String logs=captured.list.stream().map(e->e.getFormattedMessage()+(e.getThrowableProxy()==null?"":ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(e.getThrowableProxy()))).collect(java.util.stream.Collectors.joining("\n"));
   for(String secret:List.of(password,access(a),refresh(a),verification,reset)){assertFalse(dto.contains(secret),"Normal DTO must not contain an auth secret");assertFalse(logs.contains(secret),"Application log must not contain an auth secret");}
   for(String field:List.of("passwordHash","refreshToken","accessToken","tokenHash"))assertFalse(json.readTree(dto).has(field));
  }finally{logger.detachAppender(captured);captured.stop();}
 }

}
