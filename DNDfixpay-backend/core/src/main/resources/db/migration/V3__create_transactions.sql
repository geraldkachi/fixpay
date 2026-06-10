-- V3: transactions ledger
CREATE TABLE IF NOT EXISTS transactions (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID           NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    tenant_id       UUID           NOT NULL,
    type            VARCHAR(30)    NOT NULL
                    CHECK (type IN ('credit','debit','reversal','fee','funding')),
    amount          NUMERIC(18,2)  NOT NULL CHECK (amount > 0),
    balance_before  NUMERIC(18,2)  NOT NULL,
    balance_after   NUMERIC(18,2)  NOT NULL,
    reference       VARCHAR(100)   NOT NULL UNIQUE,
    external_ref    VARCHAR(200),
    description     TEXT,
    status          VARCHAR(20)    NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending','completed','failed','reversed')),
    metadata        JSONB,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_txn_wallet     ON transactions (wallet_id);
CREATE INDEX idx_txn_tenant     ON transactions (tenant_id);
CREATE INDEX idx_txn_reference  ON transactions (reference);
CREATE INDEX idx_txn_created_at ON transactions (created_at DESC);

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
