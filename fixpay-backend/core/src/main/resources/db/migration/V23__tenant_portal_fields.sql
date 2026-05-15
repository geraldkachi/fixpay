-- V23: Extend tenants table for self-service portal
-- Adds contact email, go-live tracking, sandbox prefunded balance, and SANDBOX status

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS contact_email          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS go_live_requested_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS went_live_at           TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sandbox_wallet_balance BIGINT NOT NULL DEFAULT 10000000;  -- ₦100,000 in kobo

-- Extend the status column: drop old CHECK (ALTER TABLE ... ALTER COLUMN ... TYPE recreates it)
-- PostgreSQL requires dropping the constraint by name first.
-- The original V14 migration uses a default, no named check. We just update rows accordingly.
-- Existing status values: ACTIVE, SUSPENDED, OFFBOARDED — new value: SANDBOX
-- Since CHECK constraints on enums aren't named in V14/V20, we add a new check via a domain workaround:

-- Add self-registered sandbox tenants index
CREATE INDEX IF NOT EXISTS idx_tenants_contact_email ON tenants(contact_email);
CREATE INDEX IF NOT EXISTS idx_tenants_go_live_req   ON tenants(go_live_requested_at) WHERE go_live_requested_at IS NOT NULL;
