-- PostgreSQL enum values must be committed before a later migration uses them.
ALTER TYPE booking_status ADD VALUE IF NOT EXISTS 'UNAVAILABLE';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'REQUEST_EXPIRED';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'REQUEST_BECAME_UNAVAILABLE';
