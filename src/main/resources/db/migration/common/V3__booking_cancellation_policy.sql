ALTER TABLE bookings
    ADD COLUMN cancellation_policy_version VARCHAR(20),
    ADD COLUMN grace_period_ends_at TIMESTAMPTZ,
    ADD COLUMN full_refund_until TIMESTAMPTZ,
    ADD COLUMN partial_refund_until TIMESTAMPTZ,
    ADD COLUMN partial_refund_percentage NUMERIC(5,2),
    ADD COLUMN cancellation_policy_tier VARCHAR(40),
    ADD COLUMN cancelled_by_actor_type VARCHAR(20),
    ADD COLUMN cancellation_refund_percentage NUMERIC(5,2),
    ADD COLUMN cancellation_refund_amount_eur NUMERIC(10,2),
    ADD COLUMN cancellation_retained_amount_eur NUMERIC(10,2),
    ADD COLUMN owner_cancellation_settlement_eur NUMERIC(10,2),
    ADD COLUMN cancellation_reason TEXT,
    ADD COLUMN provider_cost_responsibility VARCHAR(30),
    ADD COLUMN cancellation_provider_cost_eur NUMERIC(10,2),
    ADD COLUMN cancellation_provider_cost_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT cancellation_partial_percentage_valid CHECK
        (partial_refund_percentage IS NULL OR partial_refund_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT cancellation_refund_percentage_valid CHECK
        (cancellation_refund_percentage IS NULL OR cancellation_refund_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT cancellation_amounts_valid CHECK
        ((cancellation_refund_amount_eur IS NULL OR cancellation_refund_amount_eur >= 0)
         AND (cancellation_retained_amount_eur IS NULL OR cancellation_retained_amount_eur >= 0)
         AND (owner_cancellation_settlement_eur IS NULL OR owner_cancellation_settlement_eur >= 0)
         AND (cancellation_provider_cost_eur IS NULL OR cancellation_provider_cost_eur >= 0)),
    ADD CONSTRAINT cancellation_policy_tier_valid CHECK
        (cancellation_policy_tier IS NULL OR cancellation_policy_tier IN
         ('PRE_PAYMENT_WITHDRAWAL','GRACE_PERIOD','FLEXIBLE_FULL_REFUND','PARTIAL_REFUND',
          'NON_REFUNDABLE','OWNER_FULL_REFUND','ADMIN_FULL_REFUND')),
    ADD CONSTRAINT cancelled_actor_type_valid CHECK
        (cancelled_by_actor_type IS NULL OR cancelled_by_actor_type IN ('SCOUT','OWNER','ADMIN','SYSTEM','WEBHOOK')),
    ADD CONSTRAINT provider_cost_responsibility_valid CHECK
        (provider_cost_responsibility IS NULL OR provider_cost_responsibility IN
         ('NONE','OWNER_SETTLEMENT','OWNER','EEMA'));

DROP INDEX bookings_fee_reconciliation_idx;
CREATE INDEX bookings_fee_reconciliation_idx ON bookings (mollie_fee_reconciled, paid_at)
    WHERE mollie_fee_reconciled = FALSE
      AND status IN ('CONFIRMED'::booking_status, 'CANCELLED'::booking_status)
      AND total_paid_eur > 0;
