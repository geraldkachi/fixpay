-- V22: Self-service tenant portal tables
-- api_keys, webhook_endpoints, ip_whitelist_rules, settlement_accounts, tenant_kyb_submissions

-- ─── API Keys ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS api_keys (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    environment     VARCHAR(10)  NOT NULL CHECK (environment IN ('SANDBOX', 'LIVE')),
    key_prefix      VARCHAR(20)  NOT NULL,                         -- e.g. "fpk_test_" or "fpk_live_"
    key_hash        VARCHAR(64)  NOT NULL UNIQUE,                  -- SHA-256 hex of the raw key
    scopes          TEXT[]       NOT NULL DEFAULT '{}',
    last_used_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    created_by      UUID,                                          -- keycloak user id of portal user who created it
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_api_keys_tenant        ON api_keys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_hash          ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_keys_tenant_env    ON api_keys(tenant_id, environment);

-- ─── Webhook Endpoints ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    events          TEXT[]       NOT NULL DEFAULT '{}',
    signing_secret  VARCHAR(128) NOT NULL,                        -- HMAC-SHA256 signing secret
    environment     VARCHAR(10)  NOT NULL CHECK (environment IN ('SANDBOX', 'LIVE')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    failure_count   SMALLINT     NOT NULL DEFAULT 0,
    last_triggered  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_webhooks_tenant     ON webhook_endpoints(tenant_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_tenant_env ON webhook_endpoints(tenant_id, environment);

-- ─── IP Whitelist Rules ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ip_whitelist_rules (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    ip_cidr         VARCHAR(50)  NOT NULL,                        -- e.g. "192.168.1.0/24" or "203.0.113.42/32"
    label           VARCHAR(120),                                 -- human-readable description
    environment     VARCHAR(10)  NOT NULL CHECK (environment IN ('SANDBOX', 'LIVE')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ip_whitelist_tenant     ON ip_whitelist_rules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ip_whitelist_tenant_env ON ip_whitelist_rules(tenant_id, environment, active);

-- ─── Settlement Accounts ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS settlement_accounts (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bank_code       VARCHAR(10)  NOT NULL,
    bank_name       VARCHAR(120) NOT NULL,
    account_number  VARCHAR(20)  NOT NULL,
    account_name    VARCHAR(255) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'NGN',
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id)
);

-- ─── Tenant KYB Submissions ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_kyb_submissions (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    cac_number          VARCHAR(30),
    business_type       VARCHAR(50),                             -- SOLE_PROPRIETORSHIP | PARTNERSHIP | PRIVATE_LIMITED | PUBLIC_LIMITED
    registered_address  TEXT,
    state               VARCHAR(60),
    country             VARCHAR(60)  NOT NULL DEFAULT 'Nigeria',
    directors           JSONB        NOT NULL DEFAULT '[]',     -- [{name, role, nin, bvn}]
    document_urls       JSONB        NOT NULL DEFAULT '{}',     -- {cacCertificate, memorandum, directorId}
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    review_notes        TEXT,
    submitted_at        TIMESTAMPTZ,
    reviewed_by         UUID,                                   -- admin_users.id
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_kyb_status ON tenant_kyb_submissions(status);
