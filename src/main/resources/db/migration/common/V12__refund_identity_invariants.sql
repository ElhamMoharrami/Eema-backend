-- A booking owns one refund operation. Retries may progress status, not replace its identity.
ALTER TABLE bookings ADD CONSTRAINT refund_within_paid_amount CHECK (
    refund_amount_eur IS NULL OR (total_paid_eur IS NOT NULL AND refund_amount_eur <= total_paid_eur)
);

CREATE FUNCTION preserve_refund_identity() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.refund_idempotency_key IS NOT NULL AND (
        NEW.refund_idempotency_key IS DISTINCT FROM OLD.refund_idempotency_key
        OR NEW.refund_amount_eur IS DISTINCT FROM OLD.refund_amount_eur
        OR NEW.mollie_payment_id IS DISTINCT FROM OLD.mollie_payment_id
    ) THEN
        RAISE EXCEPTION 'Persisted refund identity is immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.mollie_refund_id IS NOT NULL AND NEW.mollie_refund_id IS DISTINCT FROM OLD.mollie_refund_id THEN
        RAISE EXCEPTION 'Persisted provider refund association is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER bookings_refund_identity BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION preserve_refund_identity();
