ALTER TABLE vtpass_payments
    ADD COLUMN IF NOT EXISTS provider_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS provider_message VARCHAR(255);
