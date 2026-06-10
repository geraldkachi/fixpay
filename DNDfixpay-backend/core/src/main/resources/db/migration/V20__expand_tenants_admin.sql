-- V20: Expand tenants table for SaaS multi-tenant admin capabilities
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS plan             VARCHAR(20)  NOT NULL DEFAULT 'STARTER',
    ADD COLUMN IF NOT EXISTS kyb_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS suspended_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS suspended_reason TEXT,
    ADD COLUMN IF NOT EXISTS feature_flags    JSONB        NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS whitelabel_config JSONB       NOT NULL DEFAULT '{}';

-- Migrate existing feature flags columns to jsonb, leaving old columns intact for backwards compat
UPDATE tenants SET feature_flags = jsonb_build_object(
    'billPayments',       feat_bill_payments,
    'directDebit',        feat_direct_debit,
    'walletTransfers',    feat_wallet_transfers,
    'intlAirtime',        feat_intl_airtime,
    'disputeManagement',  feat_dispute_management,
    'nibssTransfers',     feat_nibss_transfers
) WHERE feature_flags = '{}';

-- Migrate existing whitelabel config columns to jsonb
UPDATE tenants SET whitelabel_config = jsonb_build_object(
    'primaryColor',   primary_color,
    'secondaryColor', secondary_color,
    'accentColor',    accent_color,
    'logoUrl',        COALESCE(logo_url, ''),
    'faviconUrl',     COALESCE(favicon_url, ''),
    'supportEmail',   COALESCE(support_email, ''),
    'supportPhone',   COALESCE(support_phone, '')
) WHERE whitelabel_config = '{}';

-- Index for admin list queries
CREATE INDEX IF NOT EXISTS idx_tenants_status  ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_plan    ON tenants(plan);
CREATE INDEX IF NOT EXISTS idx_tenants_kyb_status ON tenants(kyb_status);
