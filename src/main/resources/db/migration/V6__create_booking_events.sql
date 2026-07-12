CREATE TABLE booking_events
(
    id         UUID PRIMARY KEY            DEFAULT gen_random_uuid(),

    booking_id UUID               NOT NULL
        REFERENCES bookings (id)
            ON DELETE CASCADE,

    event_type booking_event_type NOT NULL,

    payload    JSONB,

    actor_id   UUID,

    actor_name TEXT               NOT NULL,

    created_at TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);


CREATE INDEX booking_events_booking_idx
    ON booking_events (booking_id, created_at);


CREATE INDEX booking_events_type_idx
    ON booking_events (event_type);