-- Eema v1 has no free-booking lifecycle. Original price stays positive in every state;
-- total paid, refund, retained amount and owner payout may independently be zero.
-- Fail on incompatible historical rows; never rewrite financial history automatically.
ALTER TABLE bookings ADD CONSTRAINT bookings_minimum_venue_price
    CHECK (venue_price_eur >= 0.01);
