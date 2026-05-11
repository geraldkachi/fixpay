ALTER TABLE vtpass_payments
    ADD COLUMN IF NOT EXISTS init_idempotency_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS last_execute_idempotency_key VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vtpass_payments_init_idempotency
    ON vtpass_payments (init_idempotency_key)
    WHERE init_idempotency_key IS NOT NULL;
