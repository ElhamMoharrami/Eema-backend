package com.eema.backend.service;

import com.eema.backend.config.NotificationOutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

@Service @Slf4j
public class NotificationOutboxDispatcher {
    private final NotificationOutboxStore store;
    private final EmailService email;
    private final OneTimeTokenCodec tokens;
    private final NotificationOutboxProperties properties;

    public NotificationOutboxDispatcher(NotificationOutboxStore store, EmailService email, OneTimeTokenCodec tokens,
                                         NotificationOutboxProperties properties, MeterRegistry meters) {
        this.store=store; this.email=email; this.tokens=tokens; this.properties=properties;
        for(String status:java.util.List.of("PENDING","PROCESSING","DELIVERED","FAILED","CANCELLED"))
            io.micrometer.core.instrument.Gauge.builder("eema.notifications.outbox",store,s->s.count(status))
                    .tag("status",status).register(meters);
    }

    @Async("notificationExecutor")
    public void dispatchAsync(Runnable completed) {
        try { dispatchBatch(); }
        finally { completed.run(); }
    }

    public void dispatchBatch() {
        if (!properties.enabled() || !email.enabled()) return;
        for(int i=0;i<properties.batchSize();i++) {
            var claimed=store.claim();
            if(claimed.isEmpty()) return;
            var claim=claimed.get();
            try {
                var draft=claim.draft();
                if(claim.authNonce()!=null) {
                    String eligibility=store.authEligibility(claim);
                    if("CANCELLED".equals(eligibility)) {store.cancelled(claim);continue;}
                    if(!"VALID".equals(eligibility)) {store.failed(claim,"AUTH_TOKEN_EXPIRED",true);continue;}
                    String raw=tokens.reconstruct(claim.authNonce());
                    if(!tokens.hash(raw).equals(claim.authHash())) {store.failed(claim,"AUTH_DERIVATION_KEY_CHANGED",true);continue;}
                    draft=new EmailService.Draft(draft.recipient(),draft.subject(),
                        draft.text().replace(NotificationOutboxWriter.TOKEN_PLACEHOLDER,raw),
                        draft.html().replace(NotificationOutboxWriter.TOKEN_PLACEHOLDER,raw));
                }
                email.deliver(claim.id(),claim.sender(),draft);
            } catch(EmailService.DeliveryFailure failure) {
                store.failed(claim,failure.getMessage(),failure.permanent()); continue;
            } catch(RuntimeException failure) {
                // Sanitized category, never exception message/body/credentials.
                store.failed(claim,"DISPATCH_INTERNAL_FAILURE",false); continue;
            }
            // If local completion fails after provider acceptance, leave the lease recoverable.
            // Do not turn uncertain completion into a second immediate send.
            store.delivered(claim);
        }
    }
}
