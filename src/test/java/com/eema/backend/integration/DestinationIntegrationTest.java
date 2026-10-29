package com.eema.backend.integration;

import com.eema.backend.entity.enums.*;
import com.eema.backend.service.*;
import com.eema.backend.event.TrustNotificationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Disposable PostgreSQL and actual HTTP/service transactions; synthetic bank fixtures, never transfer. */
class DestinationIntegrationTest extends BookingCoreSupport {
    static final String IBAN_A="NL91ABNA0417164300",IBAN_B="DE89370400440532013000",IBAN_C="BE68539007547034";
    @DynamicPropertySource static void cryptoConfig(DynamicPropertyRegistry r){
        r.add("app.payout-destination.enabled",()->true);
        r.add("app.payout-destination.encryption-key",()->Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
        r.add("app.payout-destination.fingerprint-key",()->Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes()));
    }
    @BeforeAll static void fresh(){owner=null;policies.clear();}
    @Autowired PayoutDestinationCrypto crypto;
    @SpyBean MarketplaceTrustAuditService trustAudit;
    JsonNode destinationOwner;
    @BeforeEach void destinationActor()throws Exception{destinationOwner=actor("OWNER");jdbc.update("update users set email_verified=true where id=?",id(destinationOwner));acceptPolicies(destinationOwner);verifiedOwner(destinationOwner);}
    void verifiedOwner(JsonNode a){jdbc.update("insert into owner_verifications(owner_id,status,verification_method,submitted_at,reviewed_at,reviewed_by) values (?,'VERIFIED','MANUAL_REVIEW',now(),now(),?)",id(a),id(admin));}
    MockHttpServletResponse response(MockHttpServletRequestBuilder r,JsonNode a,Object body)throws Exception{
        r.contextPath("/api");if(a!=null)r.header("Authorization","Bearer "+access(a));if(body!=null)r.contentType("application/json").content(json.writeValueAsBytes(body));return mvc.perform(r).andReturn().getResponse();
    }
    JsonNode submit(JsonNode a,String holder,String iban,int expected)throws Exception{
        Map<String,Object> body=new HashMap<>();body.put("accountHolderName",holder);body.put("iban",iban);
        var r=response(post("/api/owner/payout-destinations"),a,body);assertEquals(expected,r.getStatus());return json.readTree(r.getContentAsByteArray());
    }
    UUID pending(String iban)throws Exception{return uuid(submit(destinationOwner,"QA Destination Owner",iban,200));}
    JsonNode review(UUID d,String action,Object body,int expected)throws Exception{return req(post("/api/admin/payout-destinations/"+d+"/"+action),admin,body,expected);}
    Map<String,Object> destinationRow(UUID d){return jdbc.queryForMap("select * from owner_payout_destinations where id=?",d);}
    List<Map<String,Object>> destinationAudit(UUID d){return jdbc.queryForList("select event_type,actor_id,actor_type,note from marketplace_trust_events where subject_id=? order by created_at,id",d);}
    int ownerRows(){return jdbc.queryForObject("select count(*) from owner_payout_destinations where owner_id=?",Integer.class,id(destinationOwner));}

    @Test void normalizedSubmissionEncryptsAndMasksEveryOrdinaryRead()throws Exception{
        var dto=submit(destinationOwner,"  QA Destination Owner  ","nl91 abna 0417 1643 00",200);UUID d=uuid(dto);var row=destinationRow(d);
        assertEquals("PENDING_VERIFICATION",dto.path("status").asText());assertEquals("QA Destination Owner",row.get("account_holder_name"));assertEquals("IBAN",row.get("type"));assertEquals("EUR",row.get("currency"));
        assertEquals(IBAN_A,crypto.decrypt(row.get("iban_encrypted").toString()));assertTrue(row.get("iban_encrypted").toString().startsWith("v1:"));assertFalse(row.get("iban_encrypted").toString().contains(IBAN_A));assertEquals(crypto.fingerprint(IBAN_A),row.get("iban_fingerprint"));assertEquals("4300",row.get("iban_last4"));
        for(var data:List.of(dto,req(get("/api/owner/payout-destinations"),destinationOwner,null,200),req(get("/api/admin/payout-destinations/"+d),admin,null,200),req(get("/api/admin/payout-destinations"),admin,null,200))){String value=data.toString();assertFalse(value.contains(IBAN_A));assertFalse(value.contains(row.get("iban_encrypted").toString()));assertFalse(value.contains(row.get("iban_fingerprint").toString()));assertTrue(value.contains("4300"));}
        var history=destinationAudit(d);assertEquals(1,history.size());assertEquals("PAYOUT_DESTINATION_SUBMITTED",history.getFirst().get("event_type"));assertEquals(id(destinationOwner),history.getFirst().get("actor_id"));assertNull(history.getFirst().get("note"));assertEquals(1,events.stream(TrustNotificationEvent.class).filter(e->e.subjectId().equals(d)).count());verifyNoInteractions(mollie);
    }
    @ParameterizedTest @ValueSource(strings={"blankName","name160","name161","iban80","iban81","checksum","country","length","nullIban"})
    void submissionBoundaries(String variant)throws Exception{
        String holder=switch(variant){case "blankName"->" ";case "name160"->"Q".repeat(160);case "name161"->"Q".repeat(161);default->"QA Owner";};
        String iban=switch(variant){case "iban80"->" ".repeat(62)+IBAN_A;case "iban81"->" ".repeat(63)+IBAN_A;case "checksum"->"NL90ABNA0417164300";case "country"->"GB82WEST12345698765432";case "length"->IBAN_A+"0";case "nullIban"->null;default->IBAN_A;};
        boolean valid=variant.equals("name160")||variant.equals("iban80");submit(destinationOwner,holder,iban,valid?200:400);assertEquals(valid?1:0,ownerRows());assertEquals(valid?1:0,events.stream(TrustNotificationEvent.class).filter(e->e.type()==TrustEventType.PAYOUT_DESTINATION_SUBMITTED).count());verifyNoInteractions(mollie);
    }
    @ParameterizedTest @ValueSource(strings={"email","verification","policies"})
    void submissionGatesDoNotBlockSafeOwnerHistoryRead(String gate)throws Exception{
        if(gate.equals("email"))jdbc.update("update users set email_verified=false where id=?",id(destinationOwner));
        if(gate.equals("verification"))jdbc.update("delete from owner_verifications where owner_id=?",id(destinationOwner));
        if(gate.equals("policies"))jdbc.update("delete from policy_acceptances where user_id=?",id(destinationOwner));
        req(get("/api/owner/payout-destinations"),destinationOwner,null,200);submit(destinationOwner,"QA Owner",IBAN_A,403);assertEquals(0,ownerRows());verifyNoInteractions(mollie);
    }
    @Test void roleAndOwnerScopeNeverExposeBankDetailsOrMutateState()throws Exception{
        UUID d=pending(IBAN_A);var before=destinationRow(d);var history=destinationAudit(d);
        for(var actor:Arrays.asList(null,scout,destinationOwner,other)){
            int code=actor==null?401:403;
            for(String suffix:List.of("","/reveal","/verify","/reject")){
                var request=suffix.isEmpty()?get("/api/admin/payout-destinations/"+d):post("/api/admin/payout-destinations/"+d+suffix);
                assertEquals(code,response(request,actor,suffix.equals("/reject")?Map.of("reason","QA denied"):null).getStatus());
            }
        }
        assertFalse(req(get("/api/owner/payout-destinations"),other,null,200).toString().contains(d.toString()));
        for(var actor:Arrays.asList(null,scout,admin)){assertEquals(actor==null?401:403,response(post("/api/owner/payout-destinations"),actor,Map.of("accountHolderName","QA","iban",IBAN_B)).getStatus());}
        assertEquals(before,destinationRow(d));assertEquals(history,destinationAudit(d));verifyNoInteractions(mollie);
    }
    @Test void replacementRejectionAndHistoricalDuplicatesPreserveOneVerified()throws Exception{
        UUID a=pending(IBAN_A);review(a,"verify",null,200);UUID b=pending(IBAN_B);
        assertEquals("VERIFIED",destinationRow(a).get("status"));assertTrue(req(get("/api/admin/payout-destinations/"+b),admin,null,200).path("anotherVerifiedDestinationExists").asBoolean());
        review(b,"reject",Map.of("reason","  QA review reason  "),200);assertEquals("QA review reason",destinationRow(b).get("rejection_reason"));assertEquals("VERIFIED",destinationRow(a).get("status"));submit(destinationOwner,"QA",IBAN_B,409);
        UUID c=pending(IBAN_C);review(c,"verify",Map.of("note","QA operational review"),200);assertEquals("REPLACED",destinationRow(a).get("status"));assertEquals("VERIFIED",destinationRow(c).get("status"));assertEquals(1,jdbc.queryForObject("select count(*) from owner_payout_destinations where owner_id=? and status='VERIFIED'",Integer.class,id(destinationOwner)));
        for(UUID d:List.of(a,b,c)){review(d,"verify",null,409);review(d,"reject",Map.of("reason","QA stale"),409);}submit(destinationOwner,"QA",IBAN_A,409);
        verifiedOwner(other);submit(other,"QA other",IBAN_A,200);assertEquals(3,ownerRows());
    }
    @ParameterizedTest @ValueSource(strings={"PENDING_VERIFICATION","VERIFIED","REPLACED","REJECTED"})
    void revealAllowedStatesHeadersAndAudit(String state)throws Exception{
        UUID d=pending(IBAN_A);
        if(state.equals("VERIFIED")||state.equals("REPLACED"))review(d,"verify",null,200);
        if(state.equals("REPLACED"))review(pending(IBAN_B),"verify",null,200);
        if(state.equals("REJECTED"))review(d,"reject",Map.of("reason","QA rejected"),200);
        int before=destinationAudit(d).size();long notices=events.stream(TrustNotificationEvent.class).count();var r=response(post("/api/admin/payout-destinations/"+d+"/reveal"),admin,null);
        assertEquals(state.equals("REJECTED")?409:200,r.getStatus());assertEquals(notices,events.stream(TrustNotificationEvent.class).count());
        if(!state.equals("REJECTED")){assertEquals("no-store",r.getHeader("Cache-Control"));assertEquals("no-cache",r.getHeader("Pragma"));assertEquals(IBAN_A,json.readTree(r.getContentAsByteArray()).path("iban").asText());var audit=destinationAudit(d);assertEquals(before+1,audit.size());assertEquals("PAYOUT_DESTINATION_REVEALED",audit.getLast().get("event_type"));assertNull(audit.getLast().get("note"));assertEquals(id(admin),audit.getLast().get("actor_id"));assertFalse(audit.toString().contains(IBAN_A));}else assertEquals(before,destinationAudit(d).size());
    }
    @RepeatedTest(3) void concurrentSameOwnerSubmissionIsConflictNot500()throws Exception{
        var body=Map.of("accountHolderName","QA Owner","iban",IBAN_A);
        var outcomes=race(()->response(post("/api/owner/payout-destinations"),destinationOwner,body).getStatus(),()->response(post("/api/owner/payout-destinations"),destinationOwner,body).getStatus());
        assertEquals(List.of(200,409),outcomes.stream().sorted().toList());assertEquals(1,ownerRows());assertEquals(1,jdbc.queryForObject("select count(*) from marketplace_trust_events e join owner_payout_destinations d on e.subject_id=d.id where d.owner_id=? and e.event_type='PAYOUT_DESTINATION_SUBMITTED'",Integer.class,id(destinationOwner)));
    }
    @RepeatedTest(3) void concurrentDistinctVerificationSerializesOwnerRouting()throws Exception{
        UUID a=pending(IBAN_A),b=pending(IBAN_B);
        var outcomes=race(()->response(post("/api/admin/payout-destinations/"+a+"/verify"),admin,null).getStatus(),()->response(post("/api/admin/payout-destinations/"+b+"/verify"),admin,null).getStatus());
        assertEquals(List.of(200,200),outcomes);assertEquals(Set.of("VERIFIED","REPLACED"),Set.of(destinationRow(a).get("status"),destinationRow(b).get("status")));
    }
    @Test void sameRecordVerifyRejectHasOneWinner()throws Exception{
        UUID d=pending(IBAN_A);var outcomes=race(()->response(post("/api/admin/payout-destinations/"+d+"/verify"),admin,null).getStatus(),()->response(post("/api/admin/payout-destinations/"+d+"/reject"),admin,Map.of("reason","QA concurrent review")).getStatus());assertEquals(List.of(200,409),outcomes.stream().sorted().toList());assertEquals(2,destinationAudit(d).size());
    }
    @Test void replacementAuditFailureRollsBackBothDestinations()throws Exception{
        UUID a=pending(IBAN_A);review(a,"verify",null,200);UUID b=pending(IBAN_B);var old=destinationRow(a);var pending=destinationRow(b);
        doThrow(new IllegalStateException("QA audit failure")).when(trustAudit).append(eq(TrustSubjectType.PAYOUT_DESTINATION),eq(b),eq(TrustEventType.PAYOUT_DESTINATION_VERIFIED),any(),any(),any());
        try{review(b,"verify",null,500);assertEquals(old,destinationRow(a));assertEquals(pending,destinationRow(b));assertEquals(2,destinationAudit(a).size());assertEquals(1,destinationAudit(b).size());}finally{reset(trustAudit);}
    }
    @ParameterizedTest @CsvSource({"verify,1000,200","verify,1001,400","reject,1000,200","reject,1001,400","reject,0,400"})
    void reviewTextBoundaries(String action,int length,int expected)throws Exception{
        UUID d=pending(IBAN_A);var before=destinationRow(d);review(d,action,Map.of(action.equals("verify")?"note":"reason","Q".repeat(length)),expected);
        if(expected==400){assertEquals(before,destinationRow(d));assertEquals(1,destinationAudit(d).size());}
        else assertEquals(action.equals("verify")?"VERIFIED":"REJECTED",destinationRow(d).get("status"));
    }
    @Test void clientCannotChooseOwnerStatusCurrencyOrTypeAndAdminFiltersAreExact()throws Exception{
        UUID d=uuid(req(post("/api/owner/payout-destinations"),destinationOwner,Map.of("accountHolderName","QA Owner","iban",IBAN_A,"ownerId",id(other),"status","VERIFIED","currency","USD","type","CARD"),200));
        var row=destinationRow(d);assertEquals(id(destinationOwner),row.get("owner_id"));assertEquals("PENDING_VERIFICATION",row.get("status"));assertEquals("EUR",row.get("currency"));assertEquals("IBAN",row.get("type"));
        var list=req(get("/api/admin/payout-destinations").param("ownerId",id(destinationOwner).toString()).param("status","PENDING_VERIFICATION").param("destinationId",d.toString()),admin,null,200);assertEquals(1,list.path("content").size());assertEquals(d.toString(),list.path("content").get(0).path("id").asText());
        assertEquals(0,req(get("/api/admin/payout-destinations").param("ownerId",id(other).toString()).param("destinationId",d.toString()),admin,null,200).path("content").size());
    }
    @Test void databaseRejectsInvalidStateRoutingCurrencyAndHistoricalDuplicates()throws Exception{
        UUID d=pending(IBAN_A);var before=destinationRow(d);
        for(String set:List.of("type='CARD'","currency='USD'","iban_last4='123'","status='VERIFIED'","status='REJECTED'","owner_id='"+UUID.randomUUID()+"'"))
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("update owner_payout_destinations set "+set+" where id=?",d));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into owner_payout_destinations(id,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at) select ?,owner_id,type,currency,account_holder_name,iban_encrypted,iban_fingerprint,iban_last4,status,submitted_at from owner_payout_destinations where id=?",UUID.randomUUID(),d));
        assertEquals(before,destinationRow(d));assertEquals(1,ownerRows());
    }
    @Test void revealCannotSucceedWithoutItsAuditAndIsPostOnly()throws Exception{
        UUID d=pending(IBAN_A);var before=destinationAudit(d);
        assertEquals(405,response(get("/api/admin/payout-destinations/"+d+"/reveal"),admin,null).getStatus());
        doThrow(new IllegalStateException("QA reveal audit failure")).when(trustAudit).append(eq(TrustSubjectType.PAYOUT_DESTINATION),eq(d),eq(TrustEventType.PAYOUT_DESTINATION_REVEALED),any(),any(),any());
        try{var response=response(post("/api/admin/payout-destinations/"+d+"/reveal"),admin,null);assertEquals(500,response.getStatus());assertFalse(response.getContentAsString().contains(IBAN_A));assertEquals(before,destinationAudit(d));}finally{reset(trustAudit);}
    }
    List<Integer> race(Callable<Integer> a,Callable<Integer> b)throws Exception{
        var gate=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        try{var one=pool.submit(()->{gate.await(10,TimeUnit.SECONDS);return a.call();});var two=pool.submit(()->{gate.await(10,TimeUnit.SECONDS);return b.call();});return List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
}
