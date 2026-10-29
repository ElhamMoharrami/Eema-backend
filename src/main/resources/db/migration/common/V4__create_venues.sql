CREATE TABLE venues
(
    id            UUID PRIMARY KEY        DEFAULT gen_random_uuid(),

    owner_id      UUID           NOT NULL
        REFERENCES users (id) ON DELETE RESTRICT,

    name          TEXT           NOT NULL
        CHECK (char_length(name) BETWEEN 2 AND 120),

    neighbourhood TEXT           NOT NULL,

    size_m2       INTEGER        NOT NULL
        CHECK (size_m2 > 0 AND size_m2 < 10000),

    day_rate_eur  NUMERIC(10, 2) NOT NULL
        CHECK (day_rate_eur > 0),

    tags          TEXT[] NOT NULL DEFAULT '{}',

    photo_paths   TEXT[] NOT NULL DEFAULT '{}',

    clip_url      TEXT,

    description   TEXT,

    status        venue_status   NOT NULL DEFAULT 'DRAFT',

    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX venues_status_idx
    ON venues (status);

CREATE INDEX venues_owner_idx
    ON venues (owner_id);


CREATE TRIGGER venues_updated_at
    BEFORE UPDATE
    ON venues
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
