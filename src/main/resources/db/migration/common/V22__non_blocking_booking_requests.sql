ALTER TABLE bookings ADD COLUMN owner_response_expires_at TIMESTAMPTZ;

-- Existing requests receive the deterministic MVP deadline used at migration time.
UPDATE bookings
SET owner_response_expires_at = created_at + INTERVAL '24 hours'
WHERE status = 'REQUESTED'::booking_status;

ALTER TABLE bookings DROP CONSTRAINT bookings_no_overlapping_reservations;
ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlapping_reservations
    EXCLUDE USING gist (
        venue_id WITH =,
        tsrange(shoot_date + start_time, shoot_date + end_time, '[)') WITH &&
    )
    WHERE (status IN (
        'AWAITING_PAYMENT'::booking_status,
        'PENDING'::booking_status, 'PAID'::booking_status, 'CONFIRMED'::booking_status
    ));

CREATE INDEX bookings_request_expiration_idx
    ON bookings (owner_response_expires_at)
    WHERE status = 'REQUESTED'::booking_status;

ALTER TABLE bookings ADD CONSTRAINT bookings_requested_owner_response_deadline
    CHECK (status <> 'REQUESTED'::booking_status OR owner_response_expires_at IS NOT NULL);
