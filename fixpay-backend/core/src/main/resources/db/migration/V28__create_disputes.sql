-- V28: Customer dispute tracking (CBN 5-day SLA for fintech)

CREATE TABLE disputes (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID         NOT NULL REFERENCES users(id),
    tenant_id              UUID         NOT NULL,
    transaction_reference  VARCHAR(80),
    -- Snapshot of the transaction at dispute time for audit
    transaction_description VARCHAR(255),
    transaction_amount     NUMERIC(18,2),
    transaction_date       TIMESTAMPTZ,
    category               VARCHAR(50)  NOT NULL
                               CHECK (category IN ('WRONG_AMOUNT','NOT_RECEIVED','DOUBLE_CHARGE','UNAUTHORIZED','OTHER')),
    description            TEXT         NOT NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'open'
                               CHECK (status IN ('open','in_review','resolved','closed')),
    resolution             TEXT,
    sla_deadline           TIMESTAMPTZ  NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_disputes_user_id    ON disputes(user_id);
CREATE INDEX idx_disputes_status     ON disputes(status);
CREATE INDEX idx_disputes_created_at ON disputes(created_at DESC);
