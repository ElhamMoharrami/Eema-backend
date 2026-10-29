package com.eema.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AuthSessionIntegrationTest extends AuthPostgresSupport {
    @Test void ownerRegistrationCreatesNoTrustOrMarketplaceAggregates() throws Exception {
        var a=actor("OWNER");UUID id=id(a);assertEquals("OWNER",a.path("user").path("role").asText());
        assertTrue(a.path("user").path("active").asBoolean());assertFalse(a.path("user").path("emailVerified").asBoolean());
        assertEquals(a.path("user"),read("/auth/me",access(a),200));
        var trust=read("/owner/verification",access(a),200);assertEquals("UNVERIFIED",trust.path("status").asText());assertTrue(trust.path("id").isNull());
        for(String table:List.of("owner_verifications","venues","owner_payout_destinations"))assertEquals(0,rows(table,"owner_id",id),table);
        assertEquals(0,rows("policy_acceptances","user_id",id));
    }
    @Test void inactiveDuplicateStillConflicts() throws Exception {
        var a=actor("SCOUT");jdbc.update("update users set active=false where id=?",id(a));
        for(String e:List.of(email(a)," "+email(a).toUpperCase(Locale.ROOT)+" "))
            call("register",Map.of("email",e,"fullName","QA Duplicate","password",QA_PASSWORD,"role","SCOUT"),409);
        assertEquals(1,rows("refresh_tokens","user_id",id(a)));assertEquals(1,rows("email_verification_tokens","user_id",id(a)));
    }
    @Test void registrationBoundaryMatrix() throws Exception {
        List<Map.Entry<String,Object>> invalid=new ArrayList<>(List.of(Map.entry("email",""),Map.entry("email","   "),Map.entry("email","no-email"),
                Map.entry("fullName"," "),Map.entry("fullName","x"),Map.entry("fullName","x".repeat(121)),Map.entry("password","x".repeat(7)),Map.entry("password","x".repeat(73))));
        for(var entry:invalid){String e=address();var b=new HashMap<String,Object>(Map.of("email",e,"fullName","QA","password",QA_PASSWORD,"role","SCOUT"));b.put(entry.getKey(),entry.getValue());call("register",b,400);assertEquals(0,jdbc.queryForObject("select count(*) from users where email=?",Integer.class,e));}
        for(String field:List.of("email","fullName","password","role")){
            var b=new HashMap<String,Object>(Map.of("email",address(),"fullName","QA","password",QA_PASSWORD,"role","SCOUT"));b.put(field,null);call("register",b,400);b.remove(field);call("register",b,400);
        }
        for(int length:List.of(8,72))for(int name:List.of(2,120)){
            String e=address(),p="x".repeat(length);var a=call("register",Map.of("email",e,"fullName","n".repeat(name),"password",p,"role","SCOUT"),201);
            assertTrue(passwords.matches(p,jdbc.queryForObject("select password_hash from users where id=?",String.class,id(a))));
        }
        // Local part <=64 and DNS labels <=63, so only total email length changes.
        String local=UUID.randomUUID().toString()+"x".repeat(28);
        String domain="a".repeat(63)+"."+"b".repeat(63)+"."+"c".repeat(56)+".test";
        String boundary=local+"@"+domain;assertEquals(254,boundary.length());
        call("register",Map.of("email",boundary,"fullName","QA","password",QA_PASSWORD,"role","SCOUT"),201);
        call("register",Map.of("email",boundary.replace(".test",".tests"),"fullName","QA","password",QA_PASSWORD,"role","SCOUT"),400);
        // AUTH-D01: pre-launch BCrypt truncation regression. Both accepted secrets must stay distinct.
        var unicode = call("register",Map.of("email",address(),"fullName","QA","password","é".repeat(72),"role","SCOUT"),201);
        String hash = jdbc.queryForObject("select password_hash from users where id=?",String.class,id(unicode));
        String differentPassword = "é".repeat(36) + "x".repeat(36);
        assertFalse(passwords.matches(differentPassword,hash), "AUTH-D01: distinct accepted Unicode passwords share the same BCrypt input prefix");
        call("login",Map.of("email",email(unicode),"password","é".repeat(72)),200);
        call("login",Map.of("email",email(unicode),"password",differentPassword),401);
        var other = call("register",Map.of("email",address(),"fullName","QA","password",differentPassword,"role","SCOUT"),201);
        call("login",Map.of("email",email(other),"password",differentPassword),200);
        call("login",Map.of("email",email(other),"password","é".repeat(72)),401);
    }
    @Test void loginMatrixAndAnonymousMe() throws Exception {
        read("/auth/me",null,401);
        for(String role:List.of("SCOUT","OWNER","ADMIN")){
            var a=actor(role.equals("ADMIN")?"SCOUT":role);UUID id=id(a);
            if(role.equals("ADMIN"))jdbc.update("update users set role='ADMIN' where id=?",id);
            long before=rows("refresh_tokens","user_id",id);
            var success=call("login",Map.of("email"," "+email(a).toUpperCase(Locale.ROOT)+" ","password",QA_PASSWORD),200);
            assertEquals(role,success.path("user").path("role").asText());assertEquals(before+1,rows("refresh_tokens","user_id",id));
            var wrong=call("login",Map.of("email",email(a),"password","WrongFixture!"),401);
            var unknown=call("login",Map.of("email",address(),"password","WrongFixture!"),401);
            assertEquals(wrong.path("message"),unknown.path("message"));
            jdbc.update("update users set active=false where id=?",id);
            call("login",Map.of("email",email(a),"password",QA_PASSWORD),403);
            call("login",Map.of("email",email(a),"password","WrongFixture!"),401);
            assertEquals(before+1,rows("refresh_tokens","user_id",id));
        }
        for(String field:List.of("email","password")){var b=new HashMap<>(Map.of("email",address(),"password",QA_PASSWORD));b.put(field,"");call("login",b,400);}
        var malformed=mvc.perform(post("/api/auth/login").contextPath("/api").contentType("application/json").content("{")).andReturn().getResponse();assertEquals(400,malformed.getStatus());
    }
    String signed(UUID id,String issuer,String role,long expires,String key) {
        return Jwts.builder().setSubject(id.toString()).setIssuer(issuer).claim("role",role).setExpiration(new Date(expires))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(key))).compact();
    }
    @Test void jwtValidationAndDatabaseAuthority() throws Exception {
        var a=actor("SCOUT");UUID id=id(a);long future=System.currentTimeMillis()+60000;
        var claims=jwt.validateToken(access(a));assertEquals(id.toString(),claims.getSubject());assertEquals("eema",claims.getIssuer());assertEquals("SCOUT",claims.get("role"));
        for(String token:List.of("not-a-jwt",signed(id,"wrong","SCOUT",future,app.jwt().secret()),signed(id,"eema","SCOUT",System.currentTimeMillis()-1000,app.jwt().secret()),
                signed(id,"eema","SCOUT",future,Base64.getEncoder().encodeToString("a".repeat(32).getBytes())))) read("/auth/me",token,401);
        String badSubject=Jwts.builder().setSubject("not-uuid").setIssuer("eema").setExpiration(new Date(future)).signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(app.jwt().secret()))).compact();read("/auth/me",badSubject,401);
        assertEquals(401,mvc.perform(get("/api/auth/me").contextPath("/api").header("Authorization",access(a))).andReturn().getResponse().getStatus());
        String bogusRole=signed(id,"eema","SUPERADMIN",future,app.jwt().secret());assertEquals("SCOUT",read("/auth/me",bogusRole,200).path("role").asText());
        String adminDetail = "/admin/bookings/" + UUID.randomUUID();
        read(adminDetail,access(a),403);jdbc.update("update users set role='ADMIN' where id=?",id);
        assertEquals("ADMIN",read("/auth/me",access(a),200).path("role").asText());
        read(adminDetail,access(a),404); // Authorized; fixture booking does not exist.
        jdbc.update("update users set role='SCOUT' where id=?",id);read(adminDetail,access(a),403);
        jdbc.update("update users set active=false where id=?",id);read("/auth/me",access(a),401);
    }
    @Test void independentSessionsAndRotation() throws Exception {
        var a=actor("SCOUT");var b=call("login",Map.of("email",email(a),"password",QA_PASSWORD),200);
        var rotated=call("refresh",Map.of("refreshToken",refresh(a)),200);
        call("refresh",Map.of("refreshToken",refresh(a)),401);
        assertTrue(jdbc.queryForObject("select revoked from refresh_tokens where token_hash=?",Boolean.class,jwt.hashRefreshToken(refresh(a))));
        assertFalse(jdbc.queryForObject("select revoked from refresh_tokens where token_hash=?",Boolean.class,jwt.hashRefreshToken(refresh(b))));
        assertFalse(jdbc.queryForObject("select revoked from refresh_tokens where token_hash=?",Boolean.class,jwt.hashRefreshToken(refresh(rotated))));
        assertEquals(2,jdbc.queryForObject("select count(*) from refresh_tokens where user_id=? and not revoked",Integer.class,id(a)));
        call("refresh",Map.of("refreshToken",refresh(b)),200);
    }
    @Test void refreshFailureMatrix() throws Exception {
        for(String state:List.of("expired","revoked","inactive")){
            var a=actor("SCOUT");String h=jwt.hashRefreshToken(refresh(a));
            if(state.equals("expired"))expire("refresh_tokens",h);
            if(state.equals("revoked"))jdbc.update("update refresh_tokens set revoked=true where token_hash=?",h);
            if(state.equals("inactive"))jdbc.update("update users set active=false where id=?",id(a));
            call("refresh",Map.of("refreshToken",refresh(a)),state.equals("inactive")?403:401);assertEquals(1,rows("refresh_tokens","user_id",id(a)));
        }
        call("refresh",Map.of("refreshToken","unknown"),401);call("refresh",Map.of("refreshToken",""),400);
    }
    @Test void concurrentHttpRefreshHasOneWinner() throws Exception {
        var a=actor("SCOUT");var barrier=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        Callable<Integer> attempt=()->{barrier.await(5,TimeUnit.SECONDS);return mvc.perform(post("/api/auth/refresh").contextPath("/api").contentType("application/json").content(json.writeValueAsBytes(Map.of("refreshToken",refresh(a))))).andReturn().getResponse().getStatus();};
        try{var x=pool.submit(attempt);var y=pool.submit(attempt);var statuses=new ArrayList<>(List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS)));Collections.sort(statuses);assertEquals(List.of(200,401),statuses);}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        assertEquals(2,rows("refresh_tokens","user_id",id(a)));assertEquals(1,jdbc.queryForObject("select count(*) from refresh_tokens where user_id=? and not revoked",Integer.class,id(a)));
    }
}
