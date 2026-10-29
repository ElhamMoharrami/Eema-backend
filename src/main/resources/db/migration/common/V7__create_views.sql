-- Eema V1 — Utility views
-- For admin queries and debugging.
-- Not exposed directly to API clients.

CREATE OR REPLACE VIEW booking_summary AS
SELECT
    b.id,

    b.status,

    b.venue_id,
    b.scout_id,

    b.mollie_payment_id,

    b.venue_price_eur,
    b.commission_percentage,
    b.commission_eur,
    b.mollie_fee_eur,
    b.owner_payout_eur,
    b.total_paid_eur,

    b.shoot_date,
    b.start_time,
    b.end_time,

    b.created_at,
    b.paid_at,
    b.confirmed_at,
    b.cancelled_at,

    v.name AS venue_name,
    v.neighbourhood,

    u.email AS scout_email,

    (
        SELECT COUNT(*)
        FROM booking_events be
        WHERE be.booking_id = b.id
    ) AS event_count

FROM bookings b

         JOIN venues v
              ON v.id = b.venue_id

         JOIN users u
              ON u.id = b.scout_id;
