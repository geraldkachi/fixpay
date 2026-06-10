package ng.fixpay.core.tenant.dto;

import ng.fixpay.core.tenant.domain.Tenant;

import java.util.Map;
import java.util.UUID;

public record TenantConfigResponse(
        UUID    tenantId,
        String  slug,
        String  appName,
        String  primaryColor,
        String  secondaryColor,
        String  accentColor,
        String  logoUrl,
        String  faviconUrl,
        String  supportEmail,
        String  supportPhone,
        Map<String, Boolean> features
) {
    public static TenantConfigResponse from(Tenant t) {
        return new TenantConfigResponse(
                t.getId(),
                t.getSlug(),
                t.getName(),
                t.getPrimaryColor(),
                t.getSecondaryColor(),
                t.getAccentColor(),
                t.getLogoUrl(),
                t.getFaviconUrl(),
                t.getSupportEmail(),
                t.getSupportPhone(),
                Map.of(
                        "billPayments",         t.isFeatBillPayments(),
                        "directDebit",          t.isFeatDirectDebit(),
                        "walletTransfers",      t.isFeatWalletTransfers(),
                        "internationalAirtime", t.isFeatIntlAirtime(),
                        "disputeManagement",    t.isFeatDisputeManagement(),
                        "nibssTransfers",       t.isFeatNibssTransfers()
                )
        );
    }
}
