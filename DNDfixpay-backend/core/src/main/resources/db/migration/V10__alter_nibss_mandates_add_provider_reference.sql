ALTER TABLE nibss_mandates
    ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_nibss_mandates_provider_reference
    ON nibss_mandates(provider_reference);
