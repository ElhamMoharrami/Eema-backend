ALTER TABLE bookings
    ADD COLUMN accepted_at TIMESTAMPTZ,
    ADD COLUMN payment_expires_at TIMESTAMPTZ,
    ADD COLUMN declined_at TIMESTAMPTZ,
    ADD COLUMN expired_at TIMESTAMPTZ;

ALTER TABLE bookings DROP CONSTRAINT bookings_no_overlapping_reservations;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlapping_reservations
    EXCLUDE USING gist (
        venue_id WITH =,
        tsrange(shoot_date + start_time, shoot_date + end_time, '[)') WITH &&
    )
    WHERE (status IN (
        'REQUESTED'::booking_status, 'AWAITING_PAYMENT'::booking_status,
        'PENDING'::booking_status, 'PAID'::booking_status, 'CONFIRMED'::booking_status
    ));

CREATE INDEX bookings_payment_expiration_idx
    ON bookings (payment_expires_at)
    WHERE status = 'AWAITING_PAYMENT'::booking_status;
