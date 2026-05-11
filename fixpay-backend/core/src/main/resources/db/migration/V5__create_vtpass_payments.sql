CREATE TABLE IF NOT EXISTS vtpass_payments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL,
    payment_reference VARCHAR(100) NOT NULL UNIQUE,
    service_id VARCHAR(100) NOT NULL,
    biller_customer_ref VARCHAR(100) NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('WALLET','USSD','CARD','NIBSS_MANDATE')),
    payment_status VARCHAR(30) NOT NULL
        CHECK (payment_status IN ('initiated','pending_authorization','authorized','processing','completed','failed','reversed')),
    provider_status VARCHAR(30) NOT NULL,
    external_reference VARCHAR(150),
    mandate_reference VARCHAR(120),
    authorization_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vtpass_payments_user_created
    ON vtpass_payments(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_vtpass_payments_tenant
    ON vtpass_payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_vtpass_payments_status
    ON vtpass_payments(payment_status, provider_status);

CREATE TRIGGER trg_vtpass_payments_updated_at
    BEFORE UPDATE ON vtpass_payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
