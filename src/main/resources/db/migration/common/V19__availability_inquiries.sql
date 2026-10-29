CREATE TABLE availability_inquiries (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL REFERENCES venues(id),
    scout_id UUID NOT NULL REFERENCES users(id),
    requested_date DATE NOT NULL,
    requested_start_time TIME NOT NULL,
    requested_end_time TIME NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'DECLINED', 'OFFERED', 'EXPIRED', 'ACCEPTED')),
    offered_date DATE,
    offered_start_time TIME,
    offered_end_time TIME,
    offer_expires_at TIMESTAMPTZ,
    accepted_booking_id UUID UNIQUE REFERENCES bookings(id),
    declined_at TIMESTAMPTZ,
    offered_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_inquiry_requested_interval CHECK (requested_start_time < requested_end_time),
    CONSTRAINT availability_inquiry_offer_fields CHECK (
        (status IN ('OPEN', 'DECLINED') AND offered_date IS NULL AND offered_start_time IS NULL AND offered_end_time IS NULL AND offer_expires_at IS NULL)
        OR
        (status IN ('OFFERED', 'EXPIRED', 'ACCEPTED') AND offered_date IS NOT NULL AND offered_start_time IS NOT NULL AND offered_end_time IS NOT NULL AND offered_start_time < offered_end_time AND offer_expires_at IS NOT NULL)
    )
);

CREATE INDEX availability_inquiries_scout_idx ON availability_inquiries (scout_id, created_at DESC);
CREATE INDEX availability_inquiries_venue_idx ON availability_inquiries (venue_id, created_at DESC);
CREATE INDEX availability_inquiries_expiry_idx ON availability_inquiries (offer_expires_at) WHERE status = 'OFFERED';
