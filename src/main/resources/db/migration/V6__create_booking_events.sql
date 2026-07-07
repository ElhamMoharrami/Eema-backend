-- Eema V1 — Booking events (immutable audit trail)
-- APPEND-ONLY. No updates. No deletes.
-- Every state change on a booking must produce a row here.

CREATE TABLE booking_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    event_type  booking_event_type NOT NULL,
    payload     JSONB,        -- raw data: Mollie webhook body, email receipt ID, etc.
    created_by  TEXT NOT NULL, -- 'system' | 'mollie_webhook' | 'admin' | user UUID
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- No updated_at — this table is append-only
CREATE INDEX be_booking_idx ON booking_events (booking_id, created_at);
CREATE INDEX be_type_idx ON booking_events (event_type);

-- Prevent deletion of audit records at the database level
CREATE RULE no_delete_booking_events
    AS ON DELETE TO booking_events DO INSTEAD NOTHING;

-- Prevent updates of audit records at the database level
CREATE RULE no_update_booking_events
    AS ON UPDATE TO booking_events
    WHERE (NEW IS DISTINCT FROM OLD)
    DO INSTEAD NOTHING;