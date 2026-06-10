-- V1: shadow user reference table (PII lives in Keycloak)
-- Stores only the Keycloak subject (sub) + tenant context needed by the app.
CREATE TABLE IF NOT EXISTS users (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id UUID        NOT NULL UNIQUE,        -- Keycloak subject claim
    tenant_id   UUID        NOT NULL,
    phone       VARCHAR(20) NOT NULL UNIQUE,
    email       VARCHAR(255),
    kyc_status  VARCHAR(20) NOT NULL DEFAULT 'pending'
                CHECK (kyc_status IN ('pending','partial','verified','rejected')),
    tier        SMALLINT    NOT NULL DEFAULT 1
                CHECK (tier BETWEEN 1 AND 3),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_tenant ON users (tenant_id);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
