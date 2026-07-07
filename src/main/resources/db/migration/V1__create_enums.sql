-- Eema V1 — Enum types
-- All application state machines are defined here as PostgreSQL enums.
-- Adding values to enums requires ALTER TYPE — plan carefully.

CREATE TYPE venue_status AS ENUM (
    'pending',    -- submitted by owner, awaiting admin approval
    'active',     -- approved and visible in search
    'suspended'   -- hidden by admin
);

CREATE TYPE user_role AS ENUM (
    'SCOUT',   -- searches and books venues
    'OWNER',   -- lists venues
    'ADMIN'    -- platform administrator
);

CREATE TYPE booking_status AS ENUM (
    'pending',    -- checkout created, payment not yet confirmed
    'paid',       -- payment confirmed by Mollie webhook
    'confirmed',  -- shoot confirmed by both parties
    'cancelled',  -- cancelled before shoot
    'refunded'    -- payment returned to scout
);

CREATE TYPE booking_event_type AS ENUM (
    'booking_created',
    'payment_created',
    'payment_succeeded',
    'payment_failed',
    'payment_cancelled',
    'booking_confirmed',
    'cancellation_requested',
    'refund_initiated',
    'refund_completed',
    'webhook_received',
    'email_sent',
    'owner_payout_released'
);