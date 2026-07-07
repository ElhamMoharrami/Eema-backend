-- Eema V1 — Bookings table
-- All monetary amounts stored in EUR with 2 decimal places.
-- scout_contact and owner_contact stored as JSONB to survive user deletion.

CREATE TABLE bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id            UUID NOT NULL REFERENCES venues(id) ON DELETE RESTRICT,
    scout_id            UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    mollie_payment_id   TEXT UNIQUE,
    status              booking_status NOT NULL DEFAULT 'pending',

    -- Financial breakdown — immutable once created
    booking_amount_eur  NUMERIC(10, 2) NOT NULL CHECK (booking_amount_eur > 0),
    service_fee_eur     NUMERIC(10, 2) NOT NULL CHECK (service_fee_eur >= 0),
    eema_commission_eur NUMERIC(10, 2) NOT NULL CHECK (eema_commission_eur >= 0),
    owner_payout_eur    NUMERIC(10, 2) NOT NULL CHECK (owner_payout_eur > 0),

    shoot_date          DATE,
    raw_brief           TEXT,

    -- Contact snapshots — copied at booking time, survive user edits/deletion
    scout_contact       JSONB,   -- {name, email, phone}
    owner_contact       JSONB,   -- {name, email, phone}

    cala_verified       BOOLEAN NOT NULL DEFAULT FALSE,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at             TIMESTAMPTZ,
    confirmed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ
);

-- Add updated_at column for trigger
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

CREATE INDEX bookings_scout_idx ON bookings (scout_id);
CREATE INDEX bookings_venue_idx ON bookings (venue_id);
CREATE INDEX bookings_status_idx ON bookings (status);
CREATE INDEX bookings_mollie_idx ON bookings (mollie_payment_id);
CREATE INDEX bookings_date_idx ON bookings (shoot_date);

CREATE TRIGGER bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();