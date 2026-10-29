ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'PAYOUT_CALCULATED';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'PAYOUT_READY';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'PAYOUT_MARKED_PAID';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'PAYOUT_CANCELLED';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'OWNER_LIABILITY_CREATED';

CREATE TABLE owner_payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE RESTRICT,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR' CHECK (currency = 'EUR'),
    gross_retained_amount_eur NUMERIC(10,2) NOT NULL CHECK (gross_retained_amount_eur >= 0),
    commission_eur NUMERIC(10,2) NOT NULL CHECK (commission_eur >= 0),
    provider_cost_eur NUMERIC(10,2) NOT NULL CHECK (provider_cost_eur >= 0),
    payout_amount_eur NUMERIC(10,2) NOT NULL CHECK (payout_amount_eur >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CALCULATED','HELD','READY','PAID','CANCELLED')),
    hold_reason VARCHAR(40) CHECK (hold_reason IS NULL OR hold_reason IN
        ('WAITING_FOR_BOOKING_END','WAITING_FOR_PROVIDER_COST','REFUND_IN_PROGRESS')),
    eligible_at TIMESTAMPTZ NOT NULL,
    ready_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    external_transfer_reference VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT owner_payouts_booking_uq UNIQUE (booking_id),
    CONSTRAINT owner_payouts_paid_fields CHECK
        (status <> 'PAID' OR (paid_at IS NOT NULL AND btrim(external_transfer_reference) <> ''))
);
CREATE INDEX owner_payouts_status_eligible_idx ON owner_payouts (status, eligible_at);
CREATE INDEX owner_payouts_owner_idx ON owner_payouts (owner_id, created_at DESC);
CREATE TRIGGER owner_payouts_updated_at BEFORE UPDATE ON owner_payouts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE owner_financial_liabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    type VARCHAR(40) NOT NULL CHECK (type IN ('PROVIDER_COST','OWNER_PAYOUT_RECOVERY')),
    amount_eur NUMERIC(10,2) NOT NULL CHECK (amount_eur > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','SETTLED','WAIVED')),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    settled_at TIMESTAMPTZ,
    CONSTRAINT owner_liability_booking_type_uq UNIQUE (booking_id, type)
);
CREATE INDEX owner_liabilities_owner_status_idx ON owner_financial_liabilities (owner_id, status);
CREATE INDEX owner_liabilities_status_idx ON owner_financial_liabilities (status);

ALTER TABLE bookings DROP COLUMN owner_payout_status;
ALTER TABLE bookings DROP COLUMN owner_payout_paid_at;
