package com.eema.backend.integration;

import com.eema.backend.event.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AuthRecoveryIntegrationTest extends AuthPostgresSupport {
    @Test void enumerationEligibilityMatrix() throws Exception {
        for(String state:List.of("unverified","verified","inactive","unknown")){
            var a=actor("SCOUT");UUID id=id(a);String e=state.equals("unknown")?address():email(a);
            if(state.equals("verified"))jdbc.update("update users set email_verified=true where id=?",id);
            if(state.equals("inactive"))jdbc.update("update users set active=false where id=?",id);
            long v=rows("email_verification_tokens","user_id",id);
            var resend=call("resend-verification",Map.of("email"," "+e.toUpperCase(Locale.ROOT)+" "),200);
            assertEquals(com.eema.backend.service.AccountRecoveryService.RESEND_MESSAGE,resend.path("message").asText());
            assertEquals(v+(state.equals("unverified")?1:0),rows("email_verification_tokens","user_id",id));
            var forgot=call("forgot-password",Map.of("email"," "+e.toUpperCase(Locale.ROOT)+" "),200);
            assertEquals(com.eema.backend.service.AccountRecoveryService.FORGOT_MESSAGE,forgot.path("message").asText());
            boolean eligible=state.equals("unverified")||state.equals("verified");assertEquals(eligible?1:0,rows("password_reset_tokens","user_id",id));
            assertEquals(eligible?1:0,events.stream(PasswordResetRequestedEvent.class).filter(x->x.userId().equals(id)).count());
            if(eligible){String raw=resetter(id);assertTrue(codec.hash(raw).equals(jdbc.queryForObject("select token_hash from password_reset_tokens where user_id=?",String.class,id)));}
        }
    }
    @Test void verificationLifecycle() throws Exception {
        var a=actor("SCOUT");UUID id=id(a);String first=verifier(id);
        call("resend-verification",Map.of("email",email(a)),200);String replacement=verifier(id);
        assertFalse(first.equals(replacement));call("verify-email",Map.of("token",first),400);
        call("verify-email",Map.of("token",replacement),200);call("verify-email",Map.of("token",replacement),200);
        assertTrue(jdbc.queryForObject("select email_verified from users where id=?",Boolean.class,id));
        assertEquals(0,jdbc.queryForObject("select count(*) from email_verification_tokens where user_id=? and used_at is null",Integer.class,id));
        expire("email_verification_tokens",codec.hash(replacement));call("verify-email",Map.of("token",replacement),200);
        for(String state:List.of("expired","inactive")){
            var b=actor("SCOUT");String raw=verifier(id(b));
            if(state.equals("expired"))expire("email_verification_tokens",codec.hash(raw));else jdbc.update("update users set active=false where id=?",id(b));
            call("verify-email",Map.of("token",raw),400);assertFalse(jdbc.queryForObject("select email_verified from users where id=?",Boolean.class,id(b)));
        }
        call("verify-email",Map.of("token","unknown"),400);call("verify-email",Map.of("token",""),400);
    }
    @Test void resetLifecycleAndBoundaries() throws Exception {
        for(String password:List.of("x".repeat(8),"x".repeat(72)," password ")){
            var a=actor("SCOUT");UUID id=id(a);var b=call("login",Map.of("email",email(a),"password",QA_PASSWORD),200);
            String oldHash=jdbc.queryForObject("select password_hash from users where id=?",String.class,id);
            call("forgot-password",Map.of("email",email(a)),200);String raw=resetter(id);
            for(String invalid:List.of("x".repeat(7),"x".repeat(73))){call("reset-password",Map.of("token",raw,"newPassword",invalid),400);assertTrue(oldHash.equals(jdbc.queryForObject("select password_hash from users where id=?",String.class,id)));}
            call("reset-password",Map.of("token",raw,"newPassword",password),200);
            String hash=jdbc.queryForObject("select password_hash from users where id=?",String.class,id);
            assertFalse(hash.equals(oldHash));assertTrue(passwords.matches(password,hash));assertFalse(passwords.matches(QA_PASSWORD,hash));
            assertEquals(0,rows("refresh_tokens","user_id",id));
            assertEquals(0,jdbc.queryForObject("select count(*) from password_reset_tokens where user_id=? and used_at is null",Integer.class,id));
            call("reset-password",Map.of("token",raw,"newPassword",password),400);
            call("refresh",Map.of("refreshToken",refresh(a)),401);call("refresh",Map.of("refreshToken",refresh(b)),401);
            // Existing access JWT is intentionally NOT revoked by password reset.
            read("/auth/me",access(a),200);
            call("login",Map.of("email",email(a),"password",QA_PASSWORD),401);call("login",Map.of("email",email(a),"password",password),200);
            if(password.startsWith(" "))call("login",Map.of("email",email(a),"password",password.trim()),401);
        }
    }
    @Test void resetMustNotMakeDistinctUnicodePasswordsEquivalent() throws Exception {
        var a=actor("SCOUT");call("forgot-password",Map.of("email",email(a)),200);
        call("reset-password",Map.of("token",resetter(id(a)),"newPassword","é".repeat(72)),200);
        String hash=jdbc.queryForObject("select password_hash from users where id=?",String.class,id(a));
        assertTrue(hash.startsWith("$argon2id$"));
        call("login",Map.of("email",email(a),"password","é".repeat(72)),200);
        call("login",Map.of("email",email(a),"password","é".repeat(36)+"x".repeat(36)),401);
        call("login",Map.of("email",email(a),"password",QA_PASSWORD),401);
        assertFalse(passwords.matches("é".repeat(36)+"x".repeat(36),hash),
                "AUTH-D01: reset also accepts passwords beyond BCrypt's effective byte limit");
    }

    @Test void resetRejectsExpiredInactiveReplacedAndUnknownTokensWithoutMutation() throws Exception {
        for(String state:List.of("expired","inactive","replaced")){
            var a=actor("SCOUT");UUID id=id(a);call("forgot-password",Map.of("email",email(a)),200);String raw=resetter(id);
            String oldHash=jdbc.queryForObject("select password_hash from users where id=?",String.class,id);
            if(state.equals("expired"))expire("password_reset_tokens",codec.hash(raw));
            if(state.equals("inactive"))jdbc.update("update users set active=false where id=?",id);
            if(state.equals("replaced"))call("forgot-password",Map.of("email",email(a)),200);
            call("reset-password",Map.of("token",raw,"newPassword","new-fixture!"),400);
            assertTrue(oldHash.equals(jdbc.queryForObject("select password_hash from users where id=?",String.class,id)));assertEquals(1,rows("refresh_tokens","user_id",id));
        }
        call("reset-password",Map.of("token","unknown","newPassword","new-fixture!"),400);
    }
    @org.springframework.beans.factory.annotation.Autowired com.eema.backend.repository.PolicyAcceptanceRepository policyAcceptances;
    @Test void policyAcceptanceProjectionReturnsOnlyTheUsersVersionIds() throws Exception {
        var a=actor("SCOUT");var b=actor("SCOUT");UUID version=UUID.randomUUID();
        jdbc.update("insert into policy_versions(id,policy_type,version,title,effective_at,content,status) values(?,'TERMS_OF_SERVICE',?,'QA policy',now(),'Fixture','DRAFT')",version,UUID.randomUUID().toString());
        jdbc.update("insert into policy_acceptances(user_id,policy_version_id,accepted_at) values(?,?,now())",id(a),version);
        assertEquals(Set.of(version),policyAcceptances.findPolicyVersionIdByUserId(id(a)));
        assertEquals(Set.of(),policyAcceptances.findPolicyVersionIdByUserId(id(b)));
        assertFalse(read("/policies/status",access(a),200).path("allRequiredAccepted").asBoolean());
        assertFalse(read("/policies/status",access(b),200).path("allRequiredAccepted").asBoolean());
    }
    @Test void verifiedMutationBoundary() throws Exception {
        for(String role:List.of("SCOUT","OWNER")){
            var a=actor(role);UUID random=UUID.randomUUID();
            read("/auth/me",access(a),200);read(role.equals("OWNER")?"/venues/my":"/bookings",access(a),200);
            Map<String,Object> booking = Map.of("venueId",random,"shootDate",java.time.LocalDate.now().plusDays(30).toString(),"startTime","10:00","endTime","11:00","rawBrief","QA fixture","scoutContact",Map.of("name","QA Auth","email",email(a)));
            String creationPath = role.equals("SCOUT") ? "/bookings" : "/venues";
            Map<String,?> creation = role.equals("SCOUT") ? booking : Map.of("name","QA Venue","neighbourhood","QA","sizeM2",20,"hourlyRateEur",10,"minimumBookingMinutes",60,"minimumLeadTimeMinutes",60,"tags",List.of());
            assertEquals(403,mvc.perform(post("/api"+creationPath).contextPath("/api").header("Authorization","Bearer "+access(a)).contentType("application/json").content(json.writeValueAsBytes(creation))).andReturn().getResponse().getStatus());
            if(role.equals("SCOUT")) {
                var inquiryBody=Map.of("venueId",random,"requestedDate",java.time.LocalDate.now().plusDays(30).toString(),"requestedStartTime","10:00","requestedEndTime","11:00","message","QA fixture");
                assertEquals(403,mvc.perform(post("/api/availability-inquiries").contextPath("/api").header("Authorization","Bearer "+access(a)).contentType("application/json").content(json.writeValueAsBytes(inquiryBody))).andReturn().getResponse().getStatus());
            }
            for(String suffix:List.of("accept","decline","checkout")){
                var r=mvc.perform(post("/api/bookings/"+random+"/"+suffix).contextPath("/api").header("Authorization","Bearer "+access(a))).andReturn().getResponse();assertEquals(403,r.getStatus());
            }
            String inquiry=role.equals("OWNER")?"decline":"accept";
            assertEquals(403,mvc.perform(post("/api/availability-inquiries/"+random+"/"+inquiry).contextPath("/api").header("Authorization","Bearer "+access(a))).andReturn().getResponse().getStatus());
            if(role.equals("OWNER")){
                assertEquals(403,mvc.perform(post("/api/owner/verification/submit").contextPath("/api").header("Authorization","Bearer "+access(a))).andReturn().getResponse().getStatus());
                var file=new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/png",new byte[]{1,2,3});
                assertEquals(403,mvc.perform(multipart("/api/upload/venue-photo").file(file).contextPath("/api").header("Authorization","Bearer "+access(a))).andReturn().getResponse().getStatus());
                assertEquals(0,rows("owner_verifications","owner_id",id(a)));
                call("verify-email",Map.of("token",verifier(id(a))),200);
                assertEquals(200,mvc.perform(post("/api/owner/verification/submit").contextPath("/api").header("Authorization","Bearer "+access(a))).andReturn().getResponse().getStatus());
                assertEquals(1,rows("owner_verifications","owner_id",id(a)));
            }else{
                call("verify-email",Map.of("token",verifier(id(a))),200);
                assertEquals(0,rows("policy_acceptances","user_id",id(a)));
                var venueOwner=actor("OWNER");
                jdbc.update("insert into venues(id,owner_id,name,neighbourhood,size_m2,hourly_rate_eur,minimum_booking_minutes,status) values(?,?,'QA policy fixture','QA',20,10,60,'ACTIVE')",random,id(venueOwner));
                var response=mvc.perform(post("/api/bookings").contextPath("/api").header("Authorization","Bearer "+access(a)).contentType("application/json").content(json.writeValueAsBytes(booking))).andReturn().getResponse();
                assertEquals(403,response.getStatus());assertTrue(json.readTree(response.getContentAsByteArray()).path("message").asText().contains("policies"));
            }
        }
    }
}
