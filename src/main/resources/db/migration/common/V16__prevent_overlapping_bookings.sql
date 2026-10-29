CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlapping_reservations
    EXCLUDE USING gist (
        venue_id WITH =,
        tsrange(shoot_date + start_time, shoot_date + end_time, '[)') WITH &&
    )
    WHERE (status IN ('PENDING'::booking_status, 'PAID'::booking_status, 'CONFIRMED'::booking_status));
