ALTER TABLE venues
    ADD COLUMN timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Amsterdam';

ALTER TABLE venues
    ADD CONSTRAINT chk_venues_timezone_not_blank CHECK (btrim(timezone) <> '');

CREATE TABLE venue_operating_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    day_of_week VARCHAR(9) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT chk_venue_operating_hours_range CHECK (start_time < end_time),
    CONSTRAINT uq_venue_operating_hours_exact UNIQUE (venue_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_venue_operating_hours_lookup ON venue_operating_hours (venue_id, day_of_week, start_time);

CREATE TABLE venue_availability_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    override_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    override_type VARCHAR(10) NOT NULL CHECK (override_type IN ('AVAILABLE','BLOCKED')),
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_venue_override_range CHECK (
        (all_day = TRUE AND start_time IS NULL AND end_time IS NULL)
        OR (all_day = FALSE AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);

CREATE INDEX idx_venue_availability_overrides_lookup
    ON venue_availability_overrides (venue_id, override_date, start_time);
