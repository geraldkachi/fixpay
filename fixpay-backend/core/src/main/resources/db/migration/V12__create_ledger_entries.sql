-- V12: ledger_entries — immutable double-entry financial ledger
-- Every wallet balance change produces exactly two rows (DEBIT + CREDIT pair)
-- linked by correlation_id.  The wallet.balance column is a denormalised cache;
-- this table is the book of record.

CREATE TABLE IF NOT EXISTS ledger_entries (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id        UUID           NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    user_id          UUID           NOT NULL REFERENCES users(id)   ON DELETE RESTRICT,
    tenant_id        UUID           NOT NULL,
    correlation_id   VARCHAR(100)   NOT NULL,
    entry_type       VARCHAR(10)    NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount           NUMERIC(18,2)  NOT NULL CHECK (amount > 0),
    running_balance  NUMERIC(18,2)  NOT NULL,
    currency         CHAR(3)        NOT NULL DEFAULT 'NGN',
    reference        VARCHAR(150)   NOT NULL,
    description      VARCHAR(255)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Primary query pattern: full ledger trail for a wallet ordered by time
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_created
    ON ledger_entries (wallet_id, created_at ASC);

-- Lookup both legs of a double-entry pair
CREATE INDEX IF NOT EXISTS idx_ledger_correlation
    ON ledger_entries (correlation_id);

-- Lookup all entries for a payment reference
CREATE INDEX IF NOT EXISTS idx_ledger_reference
    ON ledger_entries (reference);
