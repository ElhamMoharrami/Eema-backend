package com.eema.backend.integration;

import com.eema.backend.config.AppProperties;
import com.eema.backend.config.AuthRecoveryProperties;
import com.eema.backend.dto.request.RegisterRequest;
import com.eema.backend.entity.EmailVerificationToken;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.event.EmailVerificationRequestedEvent;
import com.eema.backend.repository.EmailVerificationTokenRepository;
import com.eema.backend.security.JwtService;
import com.eema.backend.service.AuthService;
import com.eema.backend.service.EmailService;
import com.eema.backend.service.OneTimeTokenCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** AUTH-001: actual HTTP/security/transaction stack and disposable PostgreSQL, no provider calls. */
class RegistrationIntegrationTest extends AuthPostgresSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;
    @Autowired OneTimeTokenCodec codec;
    @Autowired AppProperties app;
    @Autowired AuthRecoveryProperties recovery;
    @Autowired AuthService auth;
    @Autowired ApplicationEvents events;
    @SpyBean EmailService email;
    @SpyBean EmailVerificationTokenRepository verificationTokens;
    private static final String PASSWORD = "QaFixture-2026!";

    private String freshEmail() { return "scout-a+" + UUID.randomUUID() + "@example.test"; }
    private Map<String, Object> payload(String address) {
        return new HashMap<>(Map.of("email", address, "password", PASSWORD, "fullName", "QA Scout A", "role", "SCOUT"));
    }
    private JsonNode register(Map<String, Object> body, int expectedStatus, String access) throws Exception {
        var request = post("/api/auth/register").contextPath("/api")
                .contentType("application/json").content(json.writeValueAsBytes(body));
        if (access != null) request.header("Authorization", "Bearer " + access);
        var response = mvc.perform(request).andReturn().getResponse();
        assertEquals(expectedStatus, response.getStatus());
        return json.readTree(response.getContentAsByteArray());
    }
    private long count(String table, String address) {
        String where = table.equals("users") ? "email=?" : "user_id=(select id from users where email=?)";
        return jdbc.queryForObject("select count(*) from " + table + " where " + where, Long.class, address);
    }
    private void assertCounts(String address, long expected) {
        for (String table : List.of("users", "refresh_tokens", "email_verification_tokens"))
            assertEquals(expected, count(table, address), table);
    }
    private void awaitEmail(String address) { awaitEmail(address, 1); }
    private void awaitEmail(String address, int expected) {
        // Only match recipient/subject; Mockito diagnostics must not print token-bearing arguments.
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            long n = mockingDetails(email).getInvocations().stream()
                    .filter(i -> i.getMethod().getName().equals("send") && address.equals(i.getArgument(0))).count();
            if (n == expected) return;
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); fail("Interrupted"); }
        }
        fail("Expected one notification adapter invocation");
    }

    @Test void anonymousScoutPersistsOnlyHashesAndReturnsExactPublicContract() throws Exception {
        String address = freshEmail();
        OffsetDateTime before = OffsetDateTime.now();
        var body = payload("  " + address.toUpperCase(Locale.ROOT) + "  ");
        body.put("fullName", "  QA Scout A  ");
        JsonNode response = register(body, 201, null);
        OffsetDateTime after = OffsetDateTime.now();
        Set<String> fields = new HashSet<>(); response.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("accessToken", "refreshToken", "tokenType", "expiresIn", "user"), fields);
        assertEquals("Bearer", response.path("tokenType").asText());
        assertEquals(app.jwt().accessTokenExpiryMinutes() * 60L, response.path("expiresIn").asLong());
        assertFalse(response.path("accessToken").asText().isBlank());
        JsonNode user = response.path("user");
        fields.clear(); user.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id", "email", "role", "fullName", "phone", "active", "emailVerified"), fields);
        assertEquals(address, user.path("email").asText());
        assertEquals("SCOUT", user.path("role").asText());
        assertEquals("QA Scout A", user.path("fullName").asText());
        assertTrue(user.path("active").asBoolean()); assertFalse(user.path("emailVerified").asBoolean());
        assertTrue(user.path("phone").isNull());
        UUID id = UUID.fromString(user.path("id").asText());
        assertCounts(address, 1);
        var row = jdbc.queryForMap("select * from users where id=?", id);
        assertEquals("SCOUT", row.get("role").toString());
        assertEquals("QA Scout A", row.get("full_name"));
        assertEquals(true, row.get("active")); assertEquals(false, row.get("email_verified"));
        String hash = (String) row.get("password_hash");
        assertTrue(hash.startsWith("$argon2id$v=19$m=65536,t=3,p=1$")); assertFalse(PASSWORD.equals(hash)); assertTrue(passwords.matches(PASSWORD, hash));
        var refresh = jdbc.queryForMap("select token_hash, revoked, expires_at from refresh_tokens where user_id=?", id);
        String rawRefresh = response.path("refreshToken").asText();
        assertFalse(rawRefresh.isBlank()); assertFalse(rawRefresh.equals(refresh.get("token_hash")));
        assertTrue(jwt.hashRefreshToken(rawRefresh).equals(refresh.get("token_hash")));
        assertEquals(false, refresh.get("revoked"));
        assertExpiry("refresh_tokens", id, before.plusDays(app.jwt().refreshTokenExpiryDays()), after.plusDays(app.jwt().refreshTokenExpiryDays()));
        var issued = events.stream(EmailVerificationRequestedEvent.class).filter(e -> e.userId().equals(id)).toList();
        assertEquals(1, issued.size());
        var verification = jdbc.queryForMap("select token_hash, used_at from email_verification_tokens where user_id=?", id);
        assertNull(verification.get("used_at"));
        assertFalse(issued.get(0).rawToken().equals(verification.get("token_hash")));
        assertTrue(codec.hash(issued.get(0).rawToken()).equals(verification.get("token_hash")));
        assertExpiry("email_verification_tokens", id, before.plusHours(recovery.emailVerificationTokenHours()), after.plusHours(recovery.emailVerificationTokenHours()));
        var me = mvc.perform(get("/api/auth/me").contextPath("/api").header("Authorization", "Bearer " + response.path("accessToken").asText())).andReturn().getResponse();
        assertEquals(200, me.getStatus()); assertEquals(user, json.readTree(me.getContentAsByteArray()));
        awaitEmail(address);
        assertFalse(app.resend().enabled());
    }
    private void assertExpiry(String table, UUID id, OffsetDateTime lower, OffsetDateTime upper) {
        OffsetDateTime expiry = jdbc.queryForObject("select expires_at from " + table + " where user_id=?", OffsetDateTime.class, id);
        assertNotNull(expiry); assertFalse(expiry.isBefore(lower.minusSeconds(1))); assertFalse(expiry.isAfter(upper.plusSeconds(1)));
    }

    @Test void authenticatedScoutDoesNotOwnOrOverrideNewRegistration() throws Exception {
        String first = freshEmail(), second = freshEmail();
        JsonNode actor = register(payload(first), 201, null);
        var body = payload(second);
        JsonNode created = register(body, 201, actor.path("accessToken").asText());
        assertFalse(actor.path("user").path("id").equals(created.path("user").path("id")));
        assertTrue(created.path("user").path("active").asBoolean()); assertFalse(created.path("user").path("emailVerified").asBoolean());
        assertEquals(second, created.path("user").path("email").asText());
        awaitEmail(first); awaitEmail(second);
    }

    @Test void duplicateCaseVariantIs409WithoutAdditionalTokensOrEvents() throws Exception {
        String address = freshEmail(); register(payload(address), 201, null); awaitEmail(address);
        long eventCount = events.stream(EmailVerificationRequestedEvent.class).count();
        register(payload("  " + address.toUpperCase(Locale.ROOT) + "  "), 409, null);
        assertCounts(address, 1);
        assertEquals(eventCount, events.stream(EmailVerificationRequestedEvent.class).count());
        assertEquals(1, mockingDetails(email).getInvocations().stream().filter(i -> i.getMethod().getName().equals("send") && address.equals(i.getArgument(0))).count());
    }

    @Test void rejectedRolesValidationAndMalformedJsonHaveNoSideEffects() throws Exception {
        for (var invalid : List.of(Map.entry("role", "ADMIN"), Map.entry("role", "SUPERUSER"), Map.entry("email", "not-an-email"),
                Map.entry("password", "short"), Map.entry("password", "x".repeat(73)), Map.entry("fullName", "x"))) {
            String address = freshEmail(); var body = payload(address); body.put(invalid.getKey(), invalid.getValue());
            register(body, 400, null); assertCounts(address, 0);
        }
        String address = freshEmail(); var invalidEmail = payload("scout @example.test"); register(invalidEmail, 400, null); assertCounts(address, 0);
        var missing = payload(address); missing.remove("role"); register(missing, 400, null);
        var response = mvc.perform(post("/api/auth/register").contextPath("/api").contentType("application/json").content("{")).andReturn().getResponse();
        assertEquals(400, response.getStatus());
        assertEquals(0, events.stream(EmailVerificationRequestedEvent.class).count());
        assertEquals(0, mockingDetails(email).getInvocations().stream().filter(i -> i.getMethod().getName().equals("send")).count());
    }

    @Test void directRegistrationAlsoCanonicalizesAndAllowsOwner() {
        String address = freshEmail();
        var response = auth.register(new RegisterRequest(" " + address.toUpperCase(Locale.ROOT) + " ", PASSWORD, "  QA  Owner  ", UserRole.OWNER));
        assertEquals(address, response.user().email()); assertEquals("QA  Owner", response.user().fullName());
        assertEquals(UserRole.OWNER, response.user().role()); assertCounts(address, 1); awaitEmail(address);
    }

    @Test void verificationPersistenceFailureRollsBackUserAndTokens() throws Exception {
        String address = freshEmail();
        doThrow(new IllegalStateException("Injected QA persistence failure")).when(verificationTokens).save(any(EmailVerificationToken.class));
        register(payload(address), 500, null);
        assertCounts(address, 0);
        assertEquals(0, events.stream(EmailVerificationRequestedEvent.class).count());
        assertEquals(0, mockingDetails(email).getInvocations().stream().filter(i -> i.getMethod().getName().equals("send")).count());
    }

    @Test void deliveryFailureAfterCommitDoesNotRollbackRegistration() throws Exception {
        String address = freshEmail(); CountDownLatch attempted = new CountDownLatch(1);
        doAnswer(invocation -> { attempted.countDown(); throw new IllegalStateException("Injected QA delivery failure"); })
                .when(email).send(eq(address), anyString(), anyString(), anyString());
        register(payload(address), 201, null);
        assertTrue(attempted.await(5, TimeUnit.SECONDS)); assertCounts(address, 1);
    }
    private JsonNode postAuth(String endpoint, Map<String, ?> body, int status) throws Exception {
        var response = mvc.perform(post("/api/auth/" + endpoint).contextPath("/api")
                .contentType("application/json").content(json.writeValueAsBytes(body))).andReturn().getResponse();
        assertEquals(status, response.getStatus());
        return json.readTree(response.getContentAsByteArray());
    }

    @Test void unknownRegistrationFieldsAreRejectedBeforeAnySideEffect() throws Exception {
        for (String field : List.of("emailVerified", "active", "id", "passwordHash", "ownerVerificationStatus", "createdAt", "arbitrary")) {
            String address = freshEmail(); var body = payload(address); body.put(field, "sensitive-placeholder");
            JsonNode error = register(body, 400, null);
            assertEquals("Malformed JSON request", error.path("message").asText());
            assertFalse(error.toString().contains("sensitive-placeholder"));
            assertCounts(address, 0);
        }
        assertEquals(0, events.stream(EmailVerificationRequestedEvent.class).count());
        assertEquals(0, mockingDetails(email).getInvocations().stream().filter(i -> i.getMethod().getName().equals("send")).count());
    }

    @Test void otherAuthEndpointsRejectUnknownFieldsWithSafeErrors() throws Exception {
        var bodies = Map.of(
                "login", Map.of("email", freshEmail(), "password", PASSWORD),
                "refresh", Map.of("refreshToken", "Opaque-token"),
                "verify-email", Map.of("token", "Opaque-token"),
                "resend-verification", Map.of("email", freshEmail()),
                "forgot-password", Map.of("email", freshEmail()),
                "reset-password", Map.of("token", "Opaque-token", "newPassword", PASSWORD));
        for (var entry : bodies.entrySet()) {
            var body = new HashMap<>(entry.getValue()); body.put("unexpected", "sensitive-placeholder");
            JsonNode error = postAuth(entry.getKey(), body, 400);
            assertEquals("Malformed JSON request", error.path("message").asText());
            assertFalse(error.toString().contains("sensitive-placeholder"));
        }
        assertEquals(0, events.stream(EmailVerificationRequestedEvent.class).count());
        assertEquals(0, events.stream(com.eema.backend.event.PasswordResetRequestedEvent.class).count());
        assertEquals(0, mockingDetails(email).getInvocations().stream().filter(i -> i.getMethod().getName().equals("send")).count());
    }

    @Test void normalizedLoginPreservesPasswordSpacesExactly() throws Exception {
        String address = freshEmail(); String raw = " password ";
        var body = payload(address); body.put("password", raw);
        register(body, 201, null); awaitEmail(address);
        JsonNode login = postAuth("login", Map.of("email", "  " + address.toUpperCase(Locale.ROOT) + "  ", "password", raw), 200);
        assertEquals(address, login.path("user").path("email").asText());
        postAuth("login", Map.of("email", address, "password", raw.trim()), 401);
        String hash = jdbc.queryForObject("select password_hash from users where email=?", String.class, address);
        assertTrue(passwords.matches(raw, hash)); assertFalse(passwords.matches(raw.trim(), hash));
    }

    @Test void recoveryUsesCanonicalEmailAndKeepsGenericResponses() throws Exception {
        String address = freshEmail(); JsonNode registered = register(payload(address), 201, null); awaitEmail(address);
        UUID id = UUID.fromString(registered.path("user").path("id").asText());
        String padded = "  " + address.toUpperCase(Locale.ROOT) + "  ";
        JsonNode forgot = postAuth("forgot-password", Map.of("email", padded), 200);
        assertEquals(forgot, postAuth("forgot-password", Map.of("email", freshEmail()), 200));
        assertEquals(1, count("password_reset_tokens", address));
        var resets = events.stream(com.eema.backend.event.PasswordResetRequestedEvent.class).toList();
        assertEquals(1, resets.size()); assertEquals(id, resets.get(0).userId());
        JsonNode resend = postAuth("resend-verification", Map.of("email", padded), 200);
        assertEquals(resend, postAuth("resend-verification", Map.of("email", freshEmail()), 200));
        assertEquals(2, count("email_verification_tokens", address));
        assertEquals(1, jdbc.queryForObject("select count(*) from email_verification_tokens where user_id=? and used_at is null", Integer.class, id));
        assertEquals(2, events.stream(EmailVerificationRequestedEvent.class).filter(e -> e.userId().equals(id)).count());
        jdbc.update("update users set email_verified=true where id=?", id);
        assertEquals(resend, postAuth("resend-verification", Map.of("email", padded), 200));
        assertEquals(2, count("email_verification_tokens", address));
        awaitEmail(address, 3);
    }

}
