-- Eema V1 — Users table
-- Owns all identity. Spring Security authenticates against this table.
-- Passwords are BCrypt-hashed. Never store plaintext.

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    role            user_role NOT NULL DEFAULT 'SCOUT',
    full_name       TEXT,
    phone           TEXT,
    mollie_account  TEXT,    -- only populated for OWNER role
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Email must be unique and is used as login identifier
CREATE UNIQUE INDEX users_email_idx ON users (LOWER(email));
CREATE INDEX users_role_idx ON users (role);

-- Auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();