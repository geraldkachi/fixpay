-- V21: Admin users table — FixPay staff accounts mapped from Keycloak
CREATE TABLE IF NOT EXISTS admin_users (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id   VARCHAR(255) NOT NULL UNIQUE,
    username           VARCHAR(100) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    role               VARCHAR(50)  NOT NULL,           -- PLATFORM_ADMIN | SUPPORT_AGENT | COMPLIANCE_OFFICER | FINANCE_OPS
    tenant_scope       UUID         REFERENCES tenants(id) ON DELETE SET NULL, -- NULL = all tenants
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_admin_users_role         ON admin_users(role);
CREATE INDEX IF NOT EXISTS idx_admin_users_tenant_scope ON admin_users(tenant_scope);
