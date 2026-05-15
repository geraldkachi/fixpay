-- V18: Processor fee schedule + maintenance flag + payment processor tracking
-- ---------------------------------------------------------------------------

-- 1. Add maintenance flag to payment_rail_config
--    Allows admins to manually remove a processor from rotation without deleting
--    its config. The PaymentRailRegistry skips configs with maintenance=TRUE.
ALTER TABLE payment_rail_config
    ADD COLUMN IF NOT EXISTS maintenance BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add processor tracking columns to vtpass_payments
--    processor_id: which rail processor handled the initiate() step (for confirmFunded)
--    processor_fee_kobo: fee charged by the processor for this transaction (used for
--    real-time revenue tracking and the future billing/settlement module)
ALTER TABLE vtpass_payments
    ADD COLUMN IF NOT EXISTS processor_id VARCHAR(60);
ALTER TABLE vtpass_payments
    ADD COLUMN IF NOT EXISTS processor_fee_kobo BIGINT NOT NULL DEFAULT 0;

-- 3. Processor fee schedule
--    One or more fee rows per payment_rail_config. The FeeCalculatorService picks the
--    active row based on effective date range and computes:
--      fee = max(min_fee_kobo, min(cap_kobo, fixed_fee_kobo + amount_kobo * percentage_fee))
--
--    fee_type:
--      FIXED      — only fixed_fee_kobo applies (percentage_fee = 0)
--      PERCENTAGE — only percentage_fee applies (fixed_fee_kobo = 0)
--      TIERED     — fixed_fee_kobo + percentage_fee both apply (most common)
CREATE TABLE IF NOT EXISTS processor_fee_schedule (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_rail_config_id UUID        NOT NULL REFERENCES payment_rail_config(id) ON DELETE CASCADE,
    fee_type               VARCHAR(20) NOT NULL CHECK (fee_type IN ('FIXED', 'PERCENTAGE', 'TIERED')),
    fixed_fee_kobo         BIGINT      NOT NULL DEFAULT 0,
    percentage_fee         NUMERIC(8, 6) NOT NULL DEFAULT 0,   -- e.g. 0.015000 = 1.5%
    cap_kobo               BIGINT,                              -- NULL = no ceiling
    min_fee_kobo           BIGINT      NOT NULL DEFAULT 0,
    effective_from         DATE        NOT NULL DEFAULT CURRENT_DATE,
    effective_to           DATE,                                -- NULL = no expiry
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fee_schedule_config_date
    ON processor_fee_schedule(payment_rail_config_id, effective_from, effective_to);
