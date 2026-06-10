-- V17: Payment Rail Config
-- Stores admin-managed configuration that maps each payment method to a
-- specific processor implementation, with per-tenant overrides and
-- priority-based fallback.

CREATE TABLE payment_rail_config (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID,                                       -- NULL = global default
    payment_method   VARCHAR(30) NOT NULL,
    processor_id     VARCHAR(60) NOT NULL,
    priority         INT         NOT NULL DEFAULT 1,
    enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    config_json      TEXT,                                       -- adapter-specific config (JSON)
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_rail_config_tenant_method_processor
        UNIQUE (tenant_id, payment_method, processor_id)
);

CREATE INDEX idx_rail_config_tenant_method
    ON payment_rail_config (tenant_id, payment_method, enabled, priority);

-- ── Global default seeds ─────────────────────────────────────────────────────
-- These defaults ensure the system is functional before an admin configures
-- tenant-specific overrides. They mirror the hard-coded behaviour that existed
-- before this rail abstraction was introduced.

INSERT INTO payment_rail_config (payment_method, processor_id, priority, enabled, config_json)
VALUES
    ('WALLET',       'internal-wallet',   1, TRUE, '{}'),
    ('NIBSS_MANDATE','nibss-direct-debit',1, TRUE, '{}'),
    ('CARD',         'paystack-card',     1, FALSE,'{}'),  -- disabled until Paystack keys are set
    ('USSD',         'paystack-ussd',     1, FALSE,'{}'),  -- disabled until Paystack keys are set
    ('BANK_TRANSFER','monnify-transfer',  1, FALSE,'{}'),  -- disabled until Monnify keys are set
    ('OPAY',         'opay-checkout',     1, FALSE,'{}');  -- disabled until OPay keys are set
