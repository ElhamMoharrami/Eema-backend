-- V10__add_gin_index_on_venues_tags.sql
-- Create a GIN index on the tags column for fast containment / overlap queries
CREATE INDEX IF NOT EXISTS venues_tags_gin_idx
    ON venues
    USING GIN (tags);
