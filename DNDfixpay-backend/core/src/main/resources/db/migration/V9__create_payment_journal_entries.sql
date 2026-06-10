CREATE TABLE IF NOT EXISTS payment_journal_entries (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES vtpass_payments(id) ON DELETE CASCADE,
    payment_reference VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    amount NUMERIC(18,2),
    balance_before NUMERIC(18,2),
    balance_after NUMERIC(18,2),
    note VARCHAR(255),
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_journal_reference_created
    ON payment_journal_entries(payment_reference, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_payment_journal_payment_id
    ON payment_journal_entries(payment_id);
