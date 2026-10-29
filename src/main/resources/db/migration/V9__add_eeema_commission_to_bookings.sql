-- Eema V1 — Add booking commission field

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS commission_eur NUMERIC(10, 2)
    NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_bookings_commission_eur'
    ) THEN
ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_commission_eur
        CHECK (commission_eur >= 0);
END IF;
END $$;