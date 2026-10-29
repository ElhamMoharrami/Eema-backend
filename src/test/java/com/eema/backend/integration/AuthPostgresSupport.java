package com.eema.backend.integration;

import com.eema.backend.config.AppProperties;
import com.eema.backend.event.*;
import com.eema.backend.security.JwtService;
import com.eema.backend.service.OneTimeTokenCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Shared existing postgres-integration/MockMvc harness. No H2 or external provider. */
@Testcontainers(disabledWithoutDocker = false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test-postgres")
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AuthPostgresSupport {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("eema_it").withUsername("eema").withPassword("eema");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("app.jwt.secret", () -> "bG9jYWwtZGV2ZWxvcG1lbnQtb25seS1qd3Qtc2VjcmV0LTMyaA==");
    }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords; @Autowired JwtService jwt; @Autowired OneTimeTokenCodec codec;
    @Autowired AppProperties app; @Autowired ApplicationEvents events;
    static final String QA_PASSWORD = "QaFixture-2026!";
    String address() { return "auth+" + UUID.randomUUID() + "@example.test"; }
    JsonNode call(String path, Map<String, ?> body, int status) throws Exception {
        var r=mvc.perform(post("/api/auth/"+path).contextPath("/api").contentType("application/json")
                .content(json.writeValueAsBytes(body))).andReturn().getResponse();
        assertEquals(status,r.getStatus(),path); return json.readTree(r.getContentAsByteArray());
    }
    JsonNode actor(String role) throws Exception {
        return call("register",Map.of("email",address(),"fullName","QA Auth","password",QA_PASSWORD,"role",role),201);
    }
    UUID id(JsonNode a) { return UUID.fromString(a.path("user").path("id").asText()); }
    String email(JsonNode a) { return a.path("user").path("email").asText(); }
    String access(JsonNode a) { return a.path("accessToken").asText(); }
    String refresh(JsonNode a) { return a.path("refreshToken").asText(); }
    JsonNode read(String path,String token,int status) throws Exception {
        var req=get("/api"+path).contextPath("/api");if(token!=null)req.header("Authorization","Bearer "+token);
        var r=mvc.perform(req).andReturn().getResponse();assertEquals(status,r.getStatus(),path);
        return json.readTree(r.getContentAsByteArray());
    }
    long rows(String table,String column,UUID id) { return jdbc.queryForObject("select count(*) from "+table+" where "+column+"=?",Long.class,id); }
    String verifier(UUID id) { return events.stream(EmailVerificationRequestedEvent.class).filter(e->e.userId().equals(id)).reduce((a,b)->b).orElseThrow().rawToken(); }
    String resetter(UUID id) { return events.stream(PasswordResetRequestedEvent.class).filter(e->e.userId().equals(id)).reduce((a,b)->b).orElseThrow().rawToken(); }
    void expire(String table,String hash) { jdbc.update("update "+table+" set created_at=now()-interval '2 days',expires_at=now()-interval '1 day' where token_hash=?",hash); }
}
