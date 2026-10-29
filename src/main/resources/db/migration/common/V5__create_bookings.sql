CREATE TABLE bookings
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    venue_id              UUID NOT NULL
        REFERENCES venues (id)
            ON DELETE RESTRICT,

    scout_id              UUID NOT NULL
        REFERENCES users (id)
            ON DELETE RESTRICT,

    mollie_payment_id     TEXT UNIQUE,

    status                booking_status NOT NULL DEFAULT 'PENDING',

    -- Financial snapshot

    venue_price_eur       NUMERIC(10, 2) NOT NULL
        CHECK (venue_price_eur >= 0),

    commission_percentage NUMERIC(5, 2) NOT NULL
        CHECK (
            commission_percentage >= 0
                AND commission_percentage <= 100
            ),

    commission_eur        NUMERIC(10, 2) NOT NULL
        CHECK (commission_eur >= 0),

    mollie_fee_eur        NUMERIC(10, 2)
        CHECK (mollie_fee_eur >= 0),

    owner_payout_eur      NUMERIC(10, 2)
        CHECK (owner_payout_eur >= 0),

    total_paid_eur        NUMERIC(10, 2) NOT NULL
        CHECK (total_paid_eur >= 0),

    -- Booking details

    shoot_date            DATE NOT NULL,

    start_time            TIME NOT NULL,

    end_time              TIME NOT NULL,

    CHECK (start_time < end_time),

    raw_brief             TEXT,

    -- Scout contact snapshot

    scout_contact_name    VARCHAR(100) NOT NULL,

    scout_contact_email   VARCHAR(255) NOT NULL,

    scout_contact_phone   VARCHAR(50),

    -- Owner contact snapshot

    owner_contact_name    VARCHAR(100) NOT NULL,

    owner_contact_email   VARCHAR(255) NOT NULL,

    owner_contact_phone   VARCHAR(50),

    -- Audit timestamps

    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    paid_at               TIMESTAMPTZ,

    confirmed_at          TIMESTAMPTZ,

    cancelled_at          TIMESTAMPTZ
);


CREATE INDEX bookings_scout_idx
    ON bookings (scout_id);


CREATE INDEX bookings_venue_idx
    ON bookings (venue_id);


CREATE INDEX bookings_status_idx
    ON bookings (status);


CREATE INDEX bookings_date_idx
    ON bookings (shoot_date);


CREATE INDEX bookings_mollie_idx
    ON bookings (mollie_payment_id);
