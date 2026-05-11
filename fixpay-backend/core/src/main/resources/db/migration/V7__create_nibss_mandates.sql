CREATE TABLE IF NOT EXISTS nibss_mandates (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL,
    mandate_reference VARCHAR(120) NOT NULL UNIQUE,
    bank_code VARCHAR(10) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    max_amount NUMERIC(18,2) NOT NULL CHECK (max_amount > 0),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('pending','active','suspended','revoked','expired','failed')),
    start_date DATE NOT NULL,
    end_date DATE,
    provider_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_nibss_mandates_user_created
    ON nibss_mandates(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_nibss_mandates_tenant
    ON nibss_mandates(tenant_id);

CREATE INDEX IF NOT EXISTS idx_nibss_mandates_status
    ON nibss_mandates(status);

CREATE TRIGGER trg_nibss_mandates_updated_at
    BEFORE UPDATE ON nibss_mandates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
