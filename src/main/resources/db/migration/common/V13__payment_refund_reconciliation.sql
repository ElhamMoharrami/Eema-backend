ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'REFUND_REQUESTED';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'REFUND_STATUS_CHANGED';

ALTER TABLE bookings
    ADD COLUMN payment_creation_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN payment_idempotency_key UUID UNIQUE,
    ADD COLUMN payment_creation_started_at TIMESTAMPTZ,
    ADD COLUMN mollie_checkout_url VARCHAR(1000),
    ADD COLUMN mollie_fee_reconciled_at TIMESTAMPTZ,
    ADD COLUMN refund_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN refund_idempotency_key UUID UNIQUE,
    ADD COLUMN mollie_refund_id TEXT UNIQUE,
    ADD COLUMN refund_amount_eur NUMERIC(10, 2),
    ADD COLUMN refund_requested_at TIMESTAMPTZ,
    ADD COLUMN refunded_at TIMESTAMPTZ,
    ADD COLUMN owner_payout_status VARCHAR(30) NOT NULL DEFAULT 'CALCULATED_NOT_PAID',
    ADD COLUMN owner_payout_paid_at TIMESTAMPTZ;

ALTER TABLE bookings
    ADD CONSTRAINT chk_payment_creation_status CHECK (payment_creation_status IN ('NOT_STARTED', 'CREATING', 'CREATED')),
    ADD CONSTRAINT chk_refund_status CHECK (refund_status IN ('NONE', 'CREATING', 'QUEUED', 'PENDING', 'PROCESSING', 'REFUNDED', 'FAILED', 'CANCELED')),
    ADD CONSTRAINT chk_refund_amount CHECK (refund_amount_eur IS NULL OR refund_amount_eur >= 0),
    ADD CONSTRAINT chk_owner_payout_status CHECK (owner_payout_status IN ('CALCULATED_NOT_PAID', 'PAID_MANUALLY'));

UPDATE bookings
SET payment_creation_status = 'CREATED'
WHERE mollie_payment_id IS NOT NULL;

CREATE INDEX bookings_fee_reconciliation_idx
    ON bookings (mollie_fee_reconciled, paid_at)
    WHERE mollie_fee_reconciled = FALSE AND status IN ('PAID', 'CONFIRMED');
