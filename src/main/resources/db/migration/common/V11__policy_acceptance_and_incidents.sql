CREATE TABLE policy_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_type VARCHAR(40) NOT NULL CHECK (policy_type IN ('TERMS_OF_SERVICE','PRIVACY_POLICY','ACCEPTABLE_USE_POLICY','PRODUCTION_POLICY','INSURANCE_REQUIREMENTS')),
    version VARCHAR(50) NOT NULL CHECK (btrim(version) <> ''),
    title VARCHAR(200) NOT NULL CHECK (btrim(title) <> ''),
    effective_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    content TEXT NOT NULL CHECK (btrim(content) <> ''),
    content_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','RETIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_policy_type_version UNIQUE (policy_type, version),
    CONSTRAINT policy_publication_fields CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND content_hash IS NULL)
        OR (status IN ('PUBLISHED','RETIRED') AND published_at IS NOT NULL AND content_hash ~ '^[0-9a-f]{64}$'))
);
CREATE UNIQUE INDEX uq_current_published_policy_type ON policy_versions(policy_type) WHERE status = 'PUBLISHED';
CREATE INDEX idx_policy_versions_type_created ON policy_versions(policy_type, created_at DESC);

CREATE TABLE policy_acceptances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    policy_version_id UUID NOT NULL REFERENCES policy_versions(id) ON DELETE RESTRICT,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    request_id VARCHAR(100),
    user_agent_summary VARCHAR(200),
    CONSTRAINT uq_policy_acceptance UNIQUE (user_id, policy_version_id)
);
CREATE INDEX idx_policy_acceptances_user ON policy_acceptances(user_id, accepted_at DESC);

CREATE TABLE booking_policy_versions (
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    policy_type VARCHAR(40) NOT NULL CHECK (policy_type IN ('TERMS_OF_SERVICE','PRIVACY_POLICY','ACCEPTABLE_USE_POLICY','PRODUCTION_POLICY','INSURANCE_REQUIREMENTS')),
    policy_version_id UUID NOT NULL REFERENCES policy_versions(id) ON DELETE RESTRICT,
    PRIMARY KEY (booking_id, policy_type),
    CONSTRAINT uq_booking_policy_version UNIQUE (booking_id, policy_version_id)
);
CREATE INDEX idx_booking_policy_version ON booking_policy_versions(policy_version_id);

CREATE TABLE incident_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    reported_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    category VARCHAR(30) NOT NULL CHECK (category IN ('PROPERTY_DAMAGE','ACCESS_PROBLEM','SAFETY','NOISE_NEIGHBOUR','MISREPRESENTATION','CONDUCT','OTHER')),
    description VARCHAR(4000) NOT NULL CHECK (char_length(btrim(description)) BETWEEN 1 AND 4000 AND description !~ '[<>]'),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','UNDER_REVIEW','RESOLVED','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    admin_resolution_note VARCHAR(2000),
    CONSTRAINT incident_resolution_fields CHECK (
        (status IN ('OPEN','UNDER_REVIEW') AND resolved_at IS NULL AND admin_resolution_note IS NULL)
        OR (status IN ('RESOLVED','CLOSED') AND resolved_at IS NOT NULL AND admin_resolution_note IS NOT NULL))
);
CREATE INDEX idx_incidents_queue ON incident_reports(status, created_at);
CREATE INDEX idx_incidents_booking ON incident_reports(booking_id, created_at);
CREATE TRIGGER incident_reports_updated_at BEFORE UPDATE ON incident_reports
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE marketplace_trust_events DROP CONSTRAINT marketplace_trust_events_subject_type_check;
ALTER TABLE marketplace_trust_events ADD CONSTRAINT marketplace_trust_events_subject_type_check
    CHECK (subject_type IN ('OWNER_VERIFICATION','VENUE','PAYOUT_DESTINATION','INCIDENT'));
ALTER TABLE marketplace_trust_events DROP CONSTRAINT marketplace_trust_events_event_type_check;
ALTER TABLE marketplace_trust_events ADD CONSTRAINT marketplace_trust_events_event_type_check
    CHECK (event_type IN (
        'OWNER_VERIFICATION_SUBMITTED','OWNER_VERIFICATION_APPROVED','OWNER_VERIFICATION_REJECTED',
        'VENUE_SUBMITTED','VENUE_APPROVED','VENUE_CHANGES_REQUESTED','VENUE_SUSPENDED','VENUE_REACTIVATED',
        'PAYOUT_DESTINATION_SUBMITTED','PAYOUT_DESTINATION_VERIFIED','PAYOUT_DESTINATION_REJECTED',
        'PAYOUT_DESTINATION_REPLACED','PAYOUT_DESTINATION_REVEALED','PAYOUT_DESTINATION_SNAPSHOTTED',
        'INCIDENT_REPORTED','INCIDENT_REVIEW_STARTED','INCIDENT_RESOLVED','INCIDENT_CLOSED'));

-- No policy content is seeded here. Production requires externally approved content
-- to be created and explicitly published through the admin API before launch.
