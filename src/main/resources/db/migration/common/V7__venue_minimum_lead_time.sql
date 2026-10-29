ALTER TABLE venues
    ADD COLUMN minimum_lead_time_minutes INTEGER,
    ADD CONSTRAINT venues_minimum_lead_time_range
        CHECK (minimum_lead_time_minutes IS NULL
            OR minimum_lead_time_minutes BETWEEN 60 AND 43200);

COMMENT ON COLUMN venues.minimum_lead_time_minutes IS
    'Minimum actual elapsed notice before a new booking commitment, in minutes';
