package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Existing template capture is local only. Outbox delivery is the sole production transport path. */
@Service
public class EmailService {
    public record Draft(String recipient, String subject, String text, String html) {
        @Override public String toString() { return "EmailDraft[redacted]"; }
    }
    private final ThreadLocal<List<Draft>> capture = new ThreadLocal<>();
    private final boolean enabled;
    private final String apiKey;
    private final String fromEmail;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final URI endpoint;

    public EmailService(AppProperties properties) { this(properties, URI.create("https://api.resend.com/emails")); }

    @Autowired
    public EmailService(AppProperties properties,
            @org.springframework.beans.factory.annotation.Value("${app.resend.api-url:https://api.resend.com/emails}") String endpoint) {
        this(properties,URI.create(endpoint));
    }

    EmailService(AppProperties properties, URI endpoint) {
        var resend = properties.resend();
        enabled = resend != null && resend.enabled();
        apiKey = resend == null ? null : resend.apiKey();
        fromEmail = resend == null ? null : resend.fromEmail();
        this.endpoint = endpoint;
        if (!"https".equals(endpoint.getScheme()) && !("http".equals(endpoint.getScheme())
                && java.util.Set.of("127.0.0.1","localhost","[::1]").contains(endpoint.getHost())))
            throw new IllegalArgumentException("Email endpoint requires HTTPS (except loopback test stub)");
        if (enabled && (!StringUtils.hasText(apiKey) || !StringUtils.hasText(fromEmail)))
            throw new IllegalStateException("Email notifications are enabled but RESEND_API_KEY or RESEND_FROM_EMAIL is missing");
    }

    @jakarta.annotation.PreDestroy
    public void close() { client.close(); }

    public boolean enabled() { return enabled; }
    public String sender() { return fromEmail; }
    public boolean isCapturing() { return capture.get() != null; }

    public List<Draft> capture(Runnable compose) {
        if (isCapturing()) throw new IllegalStateException("Nested notification composition");
        var drafts = new ArrayList<Draft>();
        capture.set(drafts);
        try { compose.run(); return List.copyOf(drafts); }
        finally { capture.remove(); }
    }

    public void send(String recipient, String subject, String text, String html) {
        if (isCapturing()) { capture.get().add(new Draft(recipient, subject, text, html)); return; }
        if (!enabled) return;
        throw new IllegalStateException("Transactional mail must be captured in the notification outbox");
    }

    public void deliver(UUID id, String sender, Draft draft) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Email transport must run outside a database transaction");
        if (!enabled) throw new DeliveryFailure("PROVIDER_DISABLED", false);
        try {
            // Stable byte ordering also across JVM restarts, independent of Map.of iteration order.
            var payload = new LinkedHashMap<String,Object>();
            payload.put("from",sender == null ? fromEmail : sender);
            payload.put("to",List.of(draft.recipient()));
            payload.put("subject",draft.subject());
            payload.put("text",draft.text());
            payload.put("html",draft.html());
            String body = json.writeValueAsString(payload);
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .header("Idempotency-Key", "eema-notification-" + id)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                var providerId=json.readTree(response.body()).path("id");
                if (!providerId.isTextual() || providerId.asText().isBlank()) throw new DeliveryFailure("INVALID_PROVIDER_RESPONSE", false);
                return;
            }
            // Never retain/log raw provider bodies. Concurrent-key conflicts are retryable;
            // an idempotency payload mismatch is terminal and needs operator attention.
            boolean retryable = status >= 500 || status == 408 || status == 429;
            if (status == 409) retryable = "concurrent_idempotent_requests".equals(json.readTree(response.body()).path("name").asText());
            throw new DeliveryFailure("PROVIDER_HTTP_" + status, !retryable);
        } catch (DeliveryFailure failure) { throw failure; }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new DeliveryFailure("INTERRUPTED", false); }
        catch (Exception failure) { throw new DeliveryFailure("PROVIDER_NETWORK_OR_RESPONSE", false); }
    }

    public static class DeliveryFailure extends RuntimeException {
        private final boolean permanent;
        public DeliveryFailure(String category, boolean permanent) { super(category); this.permanent = permanent; }
        public boolean permanent() { return permanent; }
    }
}
