CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE venue_status AS ENUM ('DRAFT', 'PENDING', 'ACTIVE', 'SUSPENDED');
CREATE TYPE user_role AS ENUM ('SCOUT', 'OWNER', 'ADMIN');
CREATE TYPE booking_status AS ENUM (
    'REQUESTED', 'AWAITING_PAYMENT', 'CONFIRMED', 'DECLINED',
    'EXPIRED', 'UNAVAILABLE', 'CANCELLED', 'REFUNDED'
);
CREATE TYPE booking_event_type AS ENUM (
    'REQUEST_SUBMITTED', 'REQUEST_ACCEPTED', 'REQUEST_DECLINED',
    'REQUEST_EXPIRED', 'REQUEST_BECAME_UNAVAILABLE', 'APPROVAL_EXPIRED',
    'BOOKING_CONFIRMED', 'PAYMENT_SUCCEEDED', 'PAYMENT_REFUNDED',
    'PAYMENT_RECONCILED', 'REFUND_REQUESTED', 'REFUND_STATUS_CHANGED',
    'LATE_PAYMENT_DETECTED', 'LATE_PAYMENT_REFUND_REQUESTED',
    'LATE_PAYMENT_REFUND_COMPLETED', 'SCOUT_CANCELLED',
    'OWNER_CANCELLED', 'ADMIN_CANCELLED'
);

CREATE FUNCTION set_updated_at() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role user_role NOT NULL DEFAULT 'SCOUT',
    full_name TEXT,
    phone TEXT,
    mollie_account TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX users_email_idx ON users (LOWER(email));
CREATE INDEX users_role_idx ON users (role);
CREATE TRIGGER users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_expiry_idx ON refresh_tokens (expires_at) WHERE NOT revoked;

CREATE TABLE venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 2 AND 120),
    neighbourhood TEXT NOT NULL,
    size_m2 INTEGER NOT NULL CHECK (size_m2 > 0 AND size_m2 < 10000),
    hourly_rate_eur NUMERIC(10,2) NOT NULL CHECK (hourly_rate_eur > 0),
    minimum_booking_minutes INTEGER NOT NULL CHECK (minimum_booking_minutes > 0),
    full_day_rate_eur NUMERIC(10,2) CHECK (full_day_rate_eur IS NULL OR full_day_rate_eur > 0),
    tags TEXT[] NOT NULL DEFAULT '{}',
    photo_paths TEXT[] NOT NULL DEFAULT '{}',
    clip_url TEXT,
    description TEXT,
    timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Amsterdam' CHECK (btrim(timezone) <> ''),
    status venue_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX venues_status_idx ON venues (status);
CREATE INDEX venues_owner_idx ON venues (owner_id);
CREATE INDEX venues_tags_gin_idx ON venues USING GIN (tags);
CREATE TRIGGER venues_updated_at BEFORE UPDATE ON venues
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE venue_operating_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    day_of_week VARCHAR(9) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT chk_venue_operating_hours_range CHECK (start_time < end_time),
    CONSTRAINT uq_venue_operating_hours_exact UNIQUE (venue_id, day_of_week, start_time, end_time)
);
CREATE INDEX venue_operating_hours_lookup_idx ON venue_operating_hours (venue_id, day_of_week, start_time);

CREATE TABLE venue_availability_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    override_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    override_type VARCHAR(10) NOT NULL CHECK (override_type IN ('AVAILABLE','BLOCKED')),
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_venue_override_range CHECK (
        (all_day AND start_time IS NULL AND end_time IS NULL)
        OR (NOT all_day AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);
CREATE INDEX venue_availability_overrides_lookup_idx
    ON venue_availability_overrides (venue_id, override_date, start_time);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE RESTRICT,
    scout_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status booking_status NOT NULL DEFAULT 'REQUESTED',
    shoot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    raw_brief TEXT,
    venue_price_eur NUMERIC(10,2) NOT NULL CHECK (venue_price_eur >= 0),
    hourly_rate_eur_snapshot NUMERIC(10,2) NOT NULL CHECK (hourly_rate_eur_snapshot > 0),
    minimum_booking_minutes_snapshot INTEGER NOT NULL CHECK (minimum_booking_minutes_snapshot > 0),
    full_day_rate_eur_snapshot NUMERIC(10,2) CHECK (full_day_rate_eur_snapshot IS NULL OR full_day_rate_eur_snapshot > 0),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    billable_minutes INTEGER NOT NULL CHECK (billable_minutes >= duration_minutes),
    commission_percentage NUMERIC(5,2) NOT NULL CHECK (commission_percentage BETWEEN 0 AND 100),
    commission_eur NUMERIC(10,2) NOT NULL CHECK (commission_eur >= 0),
    mollie_fee_eur NUMERIC(10,2) CHECK (mollie_fee_eur >= 0),
    mollie_fee_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    mollie_fee_reconciled_at TIMESTAMPTZ,
    owner_payout_eur NUMERIC(10,2) CHECK (owner_payout_eur >= 0),
    total_paid_eur NUMERIC(10,2) NOT NULL CHECK (total_paid_eur >= 0),
    mollie_payment_id TEXT UNIQUE,
    payment_creation_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (payment_creation_status IN ('NOT_STARTED','CREATING','CREATED')),
    payment_idempotency_key UUID UNIQUE,
    payment_creation_started_at TIMESTAMPTZ,
    mollie_checkout_url VARCHAR(1000),
    refund_status VARCHAR(20) NOT NULL DEFAULT 'NONE'
        CHECK (refund_status IN ('NONE','CREATING','QUEUED','PENDING','PROCESSING','REFUNDED','FAILED','CANCELED')),
    refund_idempotency_key UUID UNIQUE,
    mollie_refund_id TEXT UNIQUE,
    refund_amount_eur NUMERIC(10,2) CHECK (refund_amount_eur IS NULL OR refund_amount_eur >= 0),
    refund_requested_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    owner_payout_status VARCHAR(30) NOT NULL DEFAULT 'CALCULATED_NOT_PAID'
        CHECK (owner_payout_status IN ('CALCULATED_NOT_PAID','PAID_MANUALLY')),
    owner_payout_paid_at TIMESTAMPTZ,
    scout_contact_name VARCHAR(100) NOT NULL,
    scout_contact_email VARCHAR(255) NOT NULL,
    scout_contact_phone VARCHAR(50),
    owner_contact_name VARCHAR(100) NOT NULL,
    owner_contact_email VARCHAR(255) NOT NULL,
    owner_contact_phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    owner_response_expires_at TIMESTAMPTZ,
    payment_expires_at TIMESTAMPTZ,
    declined_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    late_payment_detected_at TIMESTAMPTZ,
    late_payment_notification_at TIMESTAMPTZ,
    CONSTRAINT bookings_interval_valid CHECK (start_time < end_time),
    CONSTRAINT bookings_requested_deadline CHECK (status <> 'REQUESTED' OR owner_response_expires_at IS NOT NULL),
    CONSTRAINT bookings_no_overlapping_reservations EXCLUDE USING gist (
        venue_id WITH =,
        tsrange(shoot_date + start_time, shoot_date + end_time, '[)') WITH &&
    ) WHERE (status IN ('AWAITING_PAYMENT'::booking_status, 'CONFIRMED'::booking_status))
);
CREATE INDEX bookings_scout_idx ON bookings (scout_id, created_at DESC);
CREATE INDEX bookings_venue_idx ON bookings (venue_id);
CREATE INDEX bookings_status_idx ON bookings (status);
CREATE INDEX bookings_date_idx ON bookings (shoot_date);
CREATE INDEX bookings_request_expiration_idx ON bookings (owner_response_expires_at)
    WHERE status = 'REQUESTED';
CREATE INDEX bookings_payment_expiration_idx ON bookings (payment_expires_at)
    WHERE status = 'AWAITING_PAYMENT';
CREATE INDEX bookings_fee_reconciliation_idx ON bookings (mollie_fee_reconciled, paid_at)
    WHERE mollie_fee_reconciled = FALSE AND status = 'CONFIRMED';

CREATE TABLE booking_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    event_type booking_event_type NOT NULL,
    payload JSONB,
    actor_id UUID,
    actor_name TEXT NOT NULL,
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('SCOUT','OWNER','ADMIN','SYSTEM','WEBHOOK')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT booking_events_actor_valid CHECK (
        (actor_type IN ('SCOUT','OWNER','ADMIN') AND actor_id IS NOT NULL)
        OR (actor_type IN ('SYSTEM','WEBHOOK') AND actor_id IS NULL)
    )
);
CREATE INDEX booking_events_booking_idx ON booking_events (booking_id, created_at);
CREATE INDEX booking_events_type_idx ON booking_events (event_type);
CREATE INDEX booking_events_actor_idx ON booking_events (actor_type, actor_id);

CREATE TABLE availability_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id),
    scout_id UUID NOT NULL REFERENCES users(id),
    requested_date DATE NOT NULL,
    requested_start_time TIME NOT NULL,
    requested_end_time TIME NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','DECLINED','OFFERED','EXPIRED','ACCEPTED')),
    offered_date DATE,
    offered_start_time TIME,
    offered_end_time TIME,
    offer_expires_at TIMESTAMPTZ,
    accepted_booking_id UUID UNIQUE REFERENCES bookings(id),
    declined_at TIMESTAMPTZ,
    offered_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_inquiry_requested_interval CHECK (requested_start_time < requested_end_time),
    CONSTRAINT availability_inquiry_offer_fields CHECK (
        (status IN ('OPEN','DECLINED') AND offered_date IS NULL AND offered_start_time IS NULL AND offered_end_time IS NULL AND offer_expires_at IS NULL)
        OR (status IN ('OFFERED','EXPIRED','ACCEPTED') AND offered_date IS NOT NULL AND offered_start_time IS NOT NULL AND offered_end_time IS NOT NULL AND offered_start_time < offered_end_time AND offer_expires_at IS NOT NULL)
    )
);
CREATE INDEX availability_inquiries_scout_idx ON availability_inquiries (scout_id, created_at DESC);
CREATE INDEX availability_inquiries_venue_idx ON availability_inquiries (venue_id, created_at DESC);
CREATE INDEX availability_inquiries_expiry_idx ON availability_inquiries (offer_expires_at) WHERE status = 'OFFERED';

CREATE VIEW booking_summary AS
SELECT b.id, b.status, b.venue_id, b.scout_id, b.mollie_payment_id,
       b.venue_price_eur, b.commission_percentage, b.commission_eur,
       b.mollie_fee_eur, b.owner_payout_eur, b.total_paid_eur,
       b.shoot_date, b.start_time, b.end_time, b.created_at, b.paid_at,
       b.confirmed_at, b.cancelled_at, v.name AS venue_name,
       v.neighbourhood, u.email AS scout_email,
       (SELECT COUNT(*) FROM booking_events e WHERE e.booking_id = b.id) AS event_count
FROM bookings b JOIN venues v ON v.id = b.venue_id JOIN users u ON u.id = b.scout_id;
