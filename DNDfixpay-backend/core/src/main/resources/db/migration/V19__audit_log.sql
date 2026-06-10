-- V19: Admin audit log for payment rail configuration changes
-- ------------------------------------------------------------
-- Captures every create / update / delete / toggle / maintenance action performed
-- by an admin on payment_rail_config and processor_fee_schedule rows.
-- Used for compliance, dispute investigation, and change-history display in the
-- admin wizard.

CREATE TABLE IF NOT EXISTS payment_rail_audit_log (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id    UUID,                       -- NULL if action was triggered by system
    action           VARCHAR(60) NOT NULL,       -- e.g. CREATE_RAIL, UPDATE_CONFIG, TOGGLE_ENABLED
    entity_type      VARCHAR(60) NOT NULL DEFAULT 'PaymentRailConfig',
    entity_id        UUID,
    before_state_json TEXT,                      -- JSON snapshot before change
    after_state_json  TEXT,                      -- JSON snapshot after change
    ip_address       VARCHAR(45),                -- IPv4 or IPv6 address of the admin
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_entity  ON payment_rail_audit_log(entity_id,       created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_admin   ON payment_rail_audit_log(admin_user_id,   created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_recent  ON payment_rail_audit_log(created_at DESC);
