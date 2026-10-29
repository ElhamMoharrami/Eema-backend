-- Registered account names permit 120 characters. Historical unnamed accounts
-- use their email as the contact-name fallback (supported up to 255 characters).
-- Widen both snapshot columns; preserve all existing text and lifecycle history.
ALTER TABLE bookings
    ALTER COLUMN scout_contact_name TYPE VARCHAR(255),
    ALTER COLUMN owner_contact_name TYPE VARCHAR(255);
