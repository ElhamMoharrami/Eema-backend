-- Eema V1 — Add booking commission field

ALTER TABLE bookings
    ADD COLUMN commission_eur NUMERIC(10, 2) NOT NULL DEFAULT 0
        CHECK (commission_eur >= 0);