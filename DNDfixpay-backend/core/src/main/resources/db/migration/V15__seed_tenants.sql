-- V15: seed platform-admin tenant + one demo fintech-client tenant
-- The demo tenant's UUID is fixed so the PWA's default X-Tenant-Slug: "demo" resolves correctly.

INSERT INTO tenants (
    id, slug, name,
    primary_color, secondary_color, accent_color,
    support_email, support_phone,
    feat_bill_payments, feat_direct_debit, feat_wallet_transfers,
    feat_intl_airtime, feat_dispute_management, feat_nibss_transfers
) VALUES
-- Platform-admin (internal FixPay operations; end users never register here)
(
    '00000000-0000-0000-0000-000000000001',
    'platform-admin',
    'FixPay Platform',
    '#1C1C1E', '#636366', '#FF9500',
    'ops@fixpay.ng', '07000000001',
    FALSE, FALSE, FALSE, FALSE, TRUE, FALSE
),
-- Demo fintech client (used during development and the default tenant for the PWA)
(
    '00000000-0000-0000-0000-000000000002',
    'demo',
    'FixPay Demo',
    '#A51D21', '#34C759', '#FF9500',
    'support@fixpay.com', '07000000000',
    TRUE, TRUE, TRUE, FALSE, TRUE, TRUE
)
ON CONFLICT (slug) DO NOTHING;
