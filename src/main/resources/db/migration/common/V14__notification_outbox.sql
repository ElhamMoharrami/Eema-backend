-- Dispatch intent commits with the domain mutation. No provider call holds these locks.
CREATE TABLE notification_outbox (
    id uuid PRIMARY KEY,
    event_key varchar(200) NOT NULL,
    notification_type varchar(80) NOT NULL,
    aggregate_type varchar(40) NOT NULL,
    aggregate_id uuid NOT NULL,
    recipient_slot integer NOT NULL CHECK (recipient_slot >= 0),
    recipient_email varchar(254) NOT NULL,
    sender_email varchar(254),
    subject text NOT NULL,
    text_body text NOT NULL,
    html_body text NOT NULL,
    auth_nonce varchar(43),
    auth_token_hash varchar(64),
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','PROCESSING','DELIVERED','FAILED','CANCELLED')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    lease_until timestamptz,
    claim_id uuid,
    delivered_at timestamptz,
    last_failure_category varchar(80),
    updated_at timestamptz NOT NULL,
    CONSTRAINT notification_event_recipient_unique UNIQUE (event_key, recipient_slot),
    CONSTRAINT notification_claim_state CHECK (
        (status = 'PROCESSING' AND claim_id IS NOT NULL AND lease_until IS NOT NULL)
        OR (status <> 'PROCESSING' AND claim_id IS NULL AND lease_until IS NULL)),
    CONSTRAINT notification_failure_state CHECK (status NOT IN ('FAILED','CANCELLED') OR last_failure_category IS NOT NULL),
    CONSTRAINT notification_delivery_state CHECK ((status = 'DELIVERED') = (delivered_at IS NOT NULL)),
    CONSTRAINT notification_auth_shape CHECK (
        (aggregate_type = 'AUTH' AND notification_type IN ('EMAIL_VERIFICATION','PASSWORD_RESET')
            AND auth_nonce IS NOT NULL AND auth_token_hash IS NOT NULL)
        OR (aggregate_type <> 'AUTH' AND auth_nonce IS NULL AND auth_token_hash IS NULL))
);
CREATE INDEX notification_due_idx ON notification_outbox(next_attempt_at, created_at, id) WHERE status = 'PENDING';
CREATE INDEX notification_lease_idx ON notification_outbox(lease_until, id) WHERE status = 'PROCESSING';
CREATE INDEX notification_failed_idx ON notification_outbox(updated_at, id) WHERE status = 'FAILED';
-- Aggregate references deliberately have no polymorphic FK: delivery history survives domain cleanup.
