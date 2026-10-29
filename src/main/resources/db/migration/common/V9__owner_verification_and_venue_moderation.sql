CREATE TYPE owner_verification_status AS ENUM ('UNVERIFIED','PENDING','VERIFIED','REJECTED');

CREATE TABLE owner_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    status owner_verification_status NOT NULL DEFAULT 'UNVERIFIED',
    owner_note VARCHAR(1000),
    review_note VARCHAR(1000),
    verification_method VARCHAR(40) CHECK (verification_method IS NULL OR verification_method IN
        ('MANUAL_REVIEW','VIDEO_CALL','BUSINESS_DOCUMENT','VENUE_CONTROL_EVIDENCE')),
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT owner_verification_review_state CHECK (
        (status IN ('UNVERIFIED','PENDING') AND reviewed_at IS NULL AND reviewed_by IS NULL)
        OR (status IN ('VERIFIED','REJECTED') AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL)
    )
);
CREATE INDEX idx_owner_verification_status ON owner_verifications(status, submitted_at);
CREATE TRIGGER owner_verifications_updated_at BEFORE UPDATE ON owner_verifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE venues ADD COLUMN submitted_for_review_at TIMESTAMPTZ;
ALTER TABLE venues ADD COLUMN reviewed_at TIMESTAMPTZ;
ALTER TABLE venues ADD COLUMN reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE venues ADD COLUMN moderation_reason VARCHAR(1000);
CREATE INDEX idx_venues_moderation_queue ON venues(status, submitted_for_review_at)
    WHERE status = 'PENDING';

CREATE TABLE marketplace_trust_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_type VARCHAR(30) NOT NULL CHECK (subject_type IN ('OWNER_VERIFICATION','VENUE')),
    subject_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'OWNER_VERIFICATION_SUBMITTED','OWNER_VERIFICATION_APPROVED','OWNER_VERIFICATION_REJECTED',
        'VENUE_SUBMITTED','VENUE_APPROVED','VENUE_CHANGES_REQUESTED','VENUE_SUSPENDED','VENUE_REACTIVATED')),
    actor_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('SCOUT','OWNER','ADMIN','SYSTEM','WEBHOOK')),
    note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trust_event_subject ON marketplace_trust_events(subject_type, subject_id, created_at);
