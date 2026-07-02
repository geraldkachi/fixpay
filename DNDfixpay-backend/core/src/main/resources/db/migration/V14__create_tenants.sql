-- V14: tenants table – one row per fintech SaaS client
-- platform_admin is a special internal tenant used to identify FixPay ops staff.
CREATE TABLE IF NOT EXISTS tenants (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug             VARCHAR(60)  NOT NULL UNIQUE,   -- URL-safe key, e.g. "demo"
    name             VARCHAR(120) NOT NULL,
    primary_color    VARCHAR(9)   NOT NULL DEFAULT '#A51D21',
    secondary_color  VARCHAR(9)   NOT NULL DEFAULT '#34C759',
    accent_color     VARCHAR(9)   NOT NULL DEFAULT '#FF9500',
    logo_url         VARCHAR(500),
    favicon_url      VARCHAR(500),
    support_email    VARCHAR(255),
    support_phone    VARCHAR(20),
    -- Feature flags (defaults match the "standard" tier)
    feat_bill_payments          BOOLEAN NOT NULL DEFAULT TRUE,
    feat_direct_debit           BOOLEAN NOT NULL DEFAULT TRUE,
    feat_wallet_transfers       BOOLEAN NOT NULL DEFAULT TRUE,
    feat_intl_airtime           BOOLEAN NOT NULL DEFAULT FALSE,
    feat_dispute_management     BOOLEAN NOT NULL DEFAULT TRUE,
    feat_nibss_transfers        BOOLEAN NOT NULL DEFAULT TRUE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
