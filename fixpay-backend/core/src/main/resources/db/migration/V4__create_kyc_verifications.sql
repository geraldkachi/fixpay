CREATE TABLE IF NOT EXISTS kyc_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    cac_registration_number VARCHAR(20) NOT NULL,
    directors_json TEXT NOT NULL,
    verification_status VARCHAR(20) NOT NULL
        CHECK (verification_status IN ('pending','verified','failed')),
    provider_reference VARCHAR(100),
    report_url VARCHAR(500),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kyc_verifications_user_created
    ON kyc_verifications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_kyc_verifications_tenant
    ON kyc_verifications(tenant_id);
