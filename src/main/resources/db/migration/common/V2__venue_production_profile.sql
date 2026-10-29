CREATE TYPE production_activity AS ENUM (
    'PHOTO', 'FILM', 'VIDEO', 'INTERVIEW', 'COMMERCIAL', 'MUSIC_VIDEO', 'CONTENT_CREATION'
);

ALTER TABLE venues
    ADD COLUMN max_crew_size INTEGER CHECK (max_crew_size > 0),
    ADD COLUMN permitted_activities production_activity[],
    ADD COLUMN prohibited_activities production_activity[],
    ADD COLUMN natural_light BOOLEAN,
    ADD COLUMN blackout_possible BOOLEAN,
    ADD COLUMN ceiling_height_m NUMERIC(5,2) CHECK (ceiling_height_m > 0),
    ADD COLUMN loading_access BOOLEAN,
    ADD COLUMN parking_available BOOLEAN,
    ADD COLUMN parking_spaces INTEGER CHECK (parking_spaces >= 0),
    ADD COLUMN floor_number INTEGER CHECK (floor_number BETWEEN -20 AND 200),
    ADD COLUMN lift_available BOOLEAN,
    ADD COLUMN wheelchair_accessible BOOLEAN,
    ADD COLUMN bathrooms_available BOOLEAN,
    ADD COLUMN sound_recording_suitable BOOLEAN,
    ADD COLUMN production_rules TEXT,
    ADD COLUMN power_details TEXT,
    ADD CONSTRAINT venues_activity_lists_disjoint CHECK (
        permitted_activities IS NULL OR prohibited_activities IS NULL
        OR NOT (permitted_activities && prohibited_activities)
    ),
    ADD CONSTRAINT venues_parking_spaces_consistent CHECK (
        parking_available IS DISTINCT FROM FALSE OR parking_spaces IS NULL OR parking_spaces = 0
    );

CREATE INDEX venues_max_crew_size_idx ON venues (max_crew_size) WHERE max_crew_size IS NOT NULL;
CREATE INDEX venues_permitted_activities_idx ON venues USING GIN (permitted_activities);
