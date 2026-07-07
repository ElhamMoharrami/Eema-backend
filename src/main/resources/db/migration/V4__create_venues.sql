-- Eema V1 — Venues table
-- owner_id references users.id (not auth.users — this is a self-managed table).
-- photo_paths stores filesystem paths, not URLs. The API generates URLs at serve time.

CREATE TABLE venues (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    name            TEXT NOT NULL CHECK (CHAR_LENGTH(name) BETWEEN 2 AND 120),
    neighbourhood   TEXT NOT NULL,
    size_m2         INTEGER NOT NULL CHECK (size_m2 > 0 AND size_m2 < 10000),
    day_rate_eur    NUMERIC(10, 2) NOT NULL CHECK (day_rate_eur > 0),
    tags            TEXT[] NOT NULL DEFAULT '{}',
    photo_paths     TEXT[] NOT NULL DEFAULT '{}',   -- filesystem paths, not URLs
    clip_url        TEXT,
    description     TEXT,
    status          venue_status NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX venues_status_idx ON venues (status);
CREATE INDEX venues_owner_idx ON venues (owner_id);
CREATE INDEX venues_tags_idx ON venues USING GIN (tags);
CREATE INDEX venues_neighbourhood_idx ON venues (neighbourhood);

CREATE TRIGGER venues_updated_at
    BEFORE UPDATE ON venues
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();