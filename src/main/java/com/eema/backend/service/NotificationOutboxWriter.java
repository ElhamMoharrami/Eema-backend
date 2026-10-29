package com.eema.backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.UUID;

/** Called synchronously BEFORE_COMMIT; insertion failure must abort the domain transaction. */
@Service
@RequiredArgsConstructor
public class NotificationOutboxWriter {
    public static final String TOKEN_PLACEHOLDER = "EEMA_AUTH_CREDENTIAL_PLACEHOLDER";
    private final JdbcTemplate jdbc;
    private final EntityManager entities;
    private final EmailService email;
    private final OneTimeTokenCodec tokens;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String aggregateType, UUID aggregateId, String type, String eventKey, Runnable compose) {
        insert(aggregateType, aggregateId, type, eventKey, compose, null, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void auth(UUID userId, String type, String rawToken, Runnable composeWithPlaceholder) {
        String hash = tokens.hash(rawToken);
        insert("AUTH", userId, type, "AUTH:" + type + ":" + hash, composeWithPlaceholder, tokens.nonce(rawToken), hash);
    }

    private void insert(String aggregateType, UUID aggregateId, String type, String key, Runnable compose,
                        String nonce, String hash) {
        entities.flush();
        // Reconciliation/repeated publication must not rewrite an existing immutable artifact.
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from notification_outbox where event_key=?)", Boolean.class, key))) return;
        var drafts = email.capture(compose);
        if (drafts.isEmpty()) throw new IllegalStateException("Lifecycle notification produced no recipient: " + type);
        var now = OffsetDateTime.now(clock);
        for (int slot = 0; slot < drafts.size(); slot++) {
            var draft = drafts.get(slot);
            if (nonce != null && (!draft.text().contains(TOKEN_PLACEHOLDER) || !draft.html().contains(TOKEN_PLACEHOLDER)
                    || draft.text().contains(tokens.reconstruct(nonce)) || draft.html().contains(tokens.reconstruct(nonce))))
                throw new IllegalStateException("Auth notification must contain only a credential placeholder");
            jdbc.update("""
                insert into notification_outbox(id,event_key,notification_type,aggregate_type,aggregate_id,
                    recipient_slot,recipient_email,sender_email,subject,text_body,html_body,auth_nonce,auth_token_hash,
                    next_attempt_at,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on conflict(event_key,recipient_slot) do nothing
                """, UUID.randomUUID(),key,type,aggregateType,aggregateId,slot,draft.recipient(),email.sender(),
                    draft.subject(),draft.text(),draft.html(),nonce,hash,now,now,now);
        }
    }
}
