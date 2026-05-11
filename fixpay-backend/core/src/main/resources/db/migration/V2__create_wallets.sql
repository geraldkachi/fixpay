-- V2: wallets — one wallet per user per currency
CREATE TABLE IF NOT EXISTS wallets (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    tenant_id    UUID           NOT NULL,
    currency     CHAR(3)        NOT NULL DEFAULT 'NGN',
    balance      NUMERIC(18,2)  NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    ledger_balance NUMERIC(18,2) NOT NULL DEFAULT 0.00 CHECK (ledger_balance >= 0),
    status       VARCHAR(20)    NOT NULL DEFAULT 'active'
                 CHECK (status IN ('active','frozen','closed')),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (user_id, currency)
);

CREATE INDEX idx_wallets_user    ON wallets (user_id);
CREATE INDEX idx_wallets_tenant  ON wallets (tenant_id);

CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
