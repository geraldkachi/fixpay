package ng.fixpay.shared.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Normalised input passed to every {@link PaymentRailAdapter} operation.
 *
 * <p>Fields that are not relevant for a given rail (e.g. {@code mandateReference} for a
 * Card rail) are simply ignored by that adapter.
 *
 * @param userId            FixPay user ID
 * @param tenantId          tenant the user belongs to
 * @param paymentReference  unique payment reference (e.g. "FP-VTP-XXXXXXXX")
 * @param amount            amount to charge
 * @param currency          ISO 4217 currency code (currently always "NGN")
 * @param billerCustomerRef meter number / smartcard / phone for the bill provider
 * @param variationCode     bundle / variation code (may be null)
 * @param mandateReference  NIBSS mandate reference (null for non-mandate rails)
 * @param callbackUrl       URL the provider should redirect to after async authorization
 * @param customerPhone     notification phone (for electricity token / TV confirmation)
 * @param subscriptionType  TV subscription type: "change" or "renew" (null otherwise)
 * @param processorConfig   adapter-specific configuration resolved from
 *                          {@code payment_rail_config.config_json}; never null, may be empty
 */
public record PaymentRailRequest(
        UUID userId,
        UUID tenantId,
        String paymentReference,
        BigDecimal amount,
        String currency,
        String billerCustomerRef,
        String variationCode,
        String mandateReference,
        String callbackUrl,
        String customerPhone,
        String subscriptionType,
        Map<String, String> processorConfig
) {}
