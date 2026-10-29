CREATE TABLE booking_events
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    booking_id UUID NOT NULL
        REFERENCES bookings (id)
            ON DELETE CASCADE,

    event_type booking_event_type NOT NULL,

    payload    JSONB,

    actor_id   UUID,

    actor_name TEXT NOT NULL,

    actor_type VARCHAR(20) NOT NULL
        CHECK (
            actor_type IN (
                           'SCOUT',
                           'OWNER',
                           'ADMIN',
                           'SYSTEM',
                           'WEBHOOK'
                )
            ),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE INDEX booking_events_booking_idx
    ON booking_events (booking_id, created_at);


CREATE INDEX booking_events_type_idx
    ON booking_events (event_type);


CREATE INDEX booking_events_actor_idx
    ON booking_events (actor_type, actor_id);