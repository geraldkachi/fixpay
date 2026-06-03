-- V27: Bank-to-bank and internal wallet transfer tracking

CREATE TABLE transfers (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL REFERENCES users(id),
    tenant_id                UUID         NOT NULL,
    reference                VARCHAR(80)  NOT NULL UNIQUE,
    transfer_type            VARCHAR(10)  NOT NULL CHECK (transfer_type IN ('BANK','WALLET')),
    -- Bank transfer fields
    recipient_account_number VARCHAR(20),
    recipient_bank_code      VARCHAR(10),
    recipient_bank_name      VARCHAR(100),
    recipient_account_name   VARCHAR(150),
    -- Internal wallet (P2P) fields
    recipient_user_id        UUID         REFERENCES users(id),
    recipient_phone          VARCHAR(20),
    -- Common
    amount                   NUMERIC(18,2) NOT NULL,
    fee                      NUMERIC(18,2) NOT NULL DEFAULT 0,
    narration                VARCHAR(255),
    status                   VARCHAR(30)  NOT NULL DEFAULT 'initiated'
                                CHECK (status IN ('initiated','processing','completed','failed','reversed')),
    provider_reference       VARCHAR(100),
    provider_response        TEXT,
    failure_reason           VARCHAR(255),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transfers_user_id    ON transfers(user_id);
CREATE INDEX idx_transfers_reference  ON transfers(reference);
CREATE INDEX idx_transfers_status     ON transfers(status);
CREATE INDEX idx_transfers_created_at ON transfers(created_at DESC);
