ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'BOOKING_RECEIPT_ISSUED';
ALTER TYPE booking_event_type ADD VALUE IF NOT EXISTS 'OWNER_PAYOUT_STATEMENT_ISSUED';

CREATE SEQUENCE booking_receipt_document_seq;
CREATE SEQUENCE owner_payout_statement_document_seq;

CREATE TABLE financial_documents (
    id UUID PRIMARY KEY,
    document_type VARCHAR(40) NOT NULL CHECK (document_type IN ('BOOKING_RECEIPT','OWNER_PAYOUT_STATEMENT')),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    owner_payout_id UUID REFERENCES owner_payouts(id) ON DELETE RESTRICT,
    recipient_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    document_number VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status = 'ISSUED'),
    template_version INTEGER NOT NULL CHECK (template_version > 0),
    storage_reference VARCHAR(300) NOT NULL UNIQUE CHECK (btrim(storage_reference) <> ''),
    issued_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT financial_document_shape CHECK (
        (document_type = 'BOOKING_RECEIPT' AND owner_payout_id IS NULL)
        OR (document_type = 'OWNER_PAYOUT_STATEMENT' AND owner_payout_id IS NOT NULL)
    )
);
CREATE UNIQUE INDEX financial_documents_booking_receipt_uq
    ON financial_documents (booking_id) WHERE document_type = 'BOOKING_RECEIPT';
CREATE UNIQUE INDEX financial_documents_payout_statement_uq
    ON financial_documents (owner_payout_id) WHERE document_type = 'OWNER_PAYOUT_STATEMENT';
CREATE INDEX financial_documents_recipient_idx ON financial_documents (recipient_user_id, issued_at DESC);
