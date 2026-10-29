CREATE TABLE owner_payout_destinations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    type VARCHAR(20) NOT NULL CHECK (type = 'IBAN'),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'EUR'),
    account_holder_name VARCHAR(160) NOT NULL,
    iban_encrypted VARCHAR(512) NOT NULL,
    iban_fingerprint VARCHAR(64) NOT NULL,
    iban_last4 VARCHAR(4) NOT NULL CHECK (iban_last4 ~ '^[0-9A-Z]{4}$'),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_VERIFICATION','VERIFIED','REJECTED','REPLACED')),
    submitted_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    verified_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    rejected_at TIMESTAMPTZ,
    rejection_reason VARCHAR(1000),
    replaced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT payout_destination_state_ck CHECK (
      (status='PENDING_VERIFICATION' AND verified_at IS NULL AND rejected_at IS NULL AND replaced_at IS NULL)
      OR (status='VERIFIED' AND verified_at IS NOT NULL AND verified_by IS NOT NULL AND rejected_at IS NULL AND replaced_at IS NULL)
      OR (status='REJECTED' AND rejected_at IS NOT NULL AND rejection_reason IS NOT NULL AND verified_at IS NULL)
      OR (status='REPLACED' AND verified_at IS NOT NULL AND verified_by IS NOT NULL AND replaced_at IS NOT NULL)
    )
);
CREATE INDEX idx_payout_destination_owner ON owner_payout_destinations(owner_id,created_at DESC);
CREATE INDEX idx_payout_destination_queue ON owner_payout_destinations(status,submitted_at);
CREATE UNIQUE INDEX owner_one_verified_destination_uq ON owner_payout_destinations(owner_id) WHERE status='VERIFIED';
CREATE UNIQUE INDEX owner_destination_fingerprint_uq ON owner_payout_destinations(owner_id,iban_fingerprint);
CREATE TRIGGER owner_payout_destinations_updated_at BEFORE UPDATE ON owner_payout_destinations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE owner_payouts ADD COLUMN payout_destination_id UUID REFERENCES owner_payout_destinations(id) ON DELETE RESTRICT;
ALTER TABLE owner_payouts DROP CONSTRAINT IF EXISTS owner_payouts_hold_reason_check;
ALTER TABLE owner_payouts ADD CONSTRAINT owner_payouts_hold_reason_check CHECK (hold_reason IS NULL OR hold_reason IN
 ('WAITING_FOR_BOOKING_END','WAITING_FOR_PROVIDER_COST','REFUND_IN_PROGRESS','WAITING_FOR_PAYOUT_DESTINATION'));
CREATE INDEX idx_owner_payout_destination ON owner_payouts(payout_destination_id);

ALTER TABLE marketplace_trust_events DROP CONSTRAINT marketplace_trust_events_subject_type_check;
ALTER TABLE marketplace_trust_events ADD CONSTRAINT marketplace_trust_events_subject_type_check
 CHECK(subject_type IN ('OWNER_VERIFICATION','VENUE','PAYOUT_DESTINATION'));
ALTER TABLE marketplace_trust_events DROP CONSTRAINT marketplace_trust_events_event_type_check;
ALTER TABLE marketplace_trust_events ADD CONSTRAINT marketplace_trust_events_event_type_check CHECK(event_type IN (
 'OWNER_VERIFICATION_SUBMITTED','OWNER_VERIFICATION_APPROVED','OWNER_VERIFICATION_REJECTED',
 'VENUE_SUBMITTED','VENUE_APPROVED','VENUE_CHANGES_REQUESTED','VENUE_SUSPENDED','VENUE_REACTIVATED',
 'PAYOUT_DESTINATION_SUBMITTED','PAYOUT_DESTINATION_VERIFIED','PAYOUT_DESTINATION_REJECTED',
 'PAYOUT_DESTINATION_REPLACED','PAYOUT_DESTINATION_REVEALED','PAYOUT_DESTINATION_SNAPSHOTTED'));
