package com.eema.backend.service;

import com.eema.backend.config.NotificationOutboxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

/** Short independent transactions. Claim UUID fences completions from expired/replaced workers. */
@Service
@RequiredArgsConstructor
public class NotificationOutboxStore {
    public record Claim(UUID id, UUID claimId, String type, UUID aggregateId, int attempts,
                        String sender, EmailService.Draft draft, String authNonce, String authHash) {}
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final EmailService email;
    private final NotificationOutboxProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Claim> claim() {
        var now = OffsetDateTime.now(clock);
        jdbc.update("""
            update notification_outbox set status='FAILED',claim_id=null,lease_until=null,
                last_failure_category='ATTEMPTS_EXHAUSTED',updated_at=?
            where id in (select id from notification_outbox where (status='PROCESSING' and lease_until<=? or status='PENDING') and attempt_count>=? order by lease_until,id limit ? for update skip locked)
            """, now,now,properties.maxAttempts(),properties.batchSize());
        UUID fence = UUID.randomUUID();
        var rows = jdbc.query("""
            with candidate as (
                select id from notification_outbox where
                    (status='PENDING' and next_attempt_at<=? and attempt_count<?) or
                    (status='PROCESSING' and lease_until<=? and attempt_count<?)
                order by next_attempt_at,created_at,id limit 1 for update skip locked
            )
            update notification_outbox n set status='PROCESSING',claim_id=?,lease_until=?,
                attempt_count=attempt_count+1,updated_at=?,sender_email=coalesce(nullif(sender_email,''),?) from candidate c where n.id=c.id returning n.*
            """, (rs,n) -> new Claim(rs.getObject("id",UUID.class),rs.getObject("claim_id",UUID.class),
                rs.getString("notification_type"),rs.getObject("aggregate_id",UUID.class),rs.getInt("attempt_count"),
                rs.getString("sender_email"),new EmailService.Draft(rs.getString("recipient_email"),rs.getString("subject"),
                rs.getString("text_body"),rs.getString("html_body")),rs.getString("auth_nonce"),rs.getString("auth_token_hash")),
                now,properties.maxAttempts(),now,properties.maxAttempts(),fence,now.plusSeconds(properties.leaseSeconds()),now,email.sender());
        return rows.stream().findFirst();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean delivered(Claim claim) { return finish(claim,"DELIVERED",null,null); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean failed(Claim claim, String category, boolean permanent) {
        boolean terminal = permanent || claim.attempts() >= properties.maxAttempts();
        return finish(claim,terminal ? "FAILED" : "PENDING",category,
                OffsetDateTime.now(clock).plusSeconds(properties.backoffSeconds(claim.attempts())));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancelled(Claim claim) { return finish(claim,"CANCELLED","AUTH_SUPERSEDED_OR_INACTIVE",null); }

    private boolean finish(Claim claim,String status,String category,OffsetDateTime retry) {
        var now=OffsetDateTime.now(clock);
        return jdbc.update("""
            update notification_outbox set status=?,last_failure_category=?,next_attempt_at=coalesce(?,next_attempt_at),
                delivered_at=?,claim_id=null,lease_until=null,updated_at=? where id=? and status='PROCESSING' and claim_id=?
            """,status,category,retry,"DELIVERED".equals(status)?now:null,now,claim.id(),claim.claimId())==1;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String authEligibility(Claim claim) {
        if (claim.authNonce()==null) return "VALID";
        String table = switch(claim.type()) {
            case "EMAIL_VERIFICATION" -> "email_verification_tokens";
            case "PASSWORD_RESET" -> "password_reset_tokens";
            default -> throw new IllegalStateException("Invalid auth notification type");
        };
        var result=jdbc.query("select t.used_at,t.expires_at,u.active,u.email from "+table+
                " t join users u on u.id=t.user_id where t.token_hash=? and t.user_id=?",
                (rs,n)-> {
                    if(rs.getObject("used_at")!=null || !rs.getBoolean("active") || !claim.draft().recipient().equalsIgnoreCase(rs.getString("email"))) return "CANCELLED";
                    return rs.getObject("expires_at",OffsetDateTime.class).isAfter(OffsetDateTime.now(clock)) ? "VALID" : "EXPIRED";
                },claim.authHash(),claim.aggregateId());
        return result.isEmpty()?"EXPIRED":result.getFirst();
    }

    public long count(String status) {
        return jdbc.queryForObject("select count(*) from notification_outbox where status=?",Long.class,status);
    }
}
