-- Eema V1 — Utility views
-- For admin queries and debugging. Not exposed directly to API clients.

CREATE OR REPLACE VIEW booking_summary AS
SELECT
    b.id,
    b.status,
    b.booking_amount_eur,
    b.eema_commission_eur,
    b.owner_payout_eur,
    b.shoot_date,
    b.created_at,
    b.paid_at,
    v.name        AS venue_name,
    v.neighbourhood,
    u.email       AS scout_email,
    (SELECT COUNT(*) FROM booking_events be WHERE be.booking_id = b.id) AS event_count
FROM bookings b
JOIN venues v ON v.id = b.venue_id
JOIN users u ON u.id = b.scout_id;