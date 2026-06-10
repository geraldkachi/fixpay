package ng.fixpay.core.payment.rail.adapters;

import ng.fixpay.shared.payment.ConfigFieldSchema;
import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Paystack card checkout rail.
 *
 * <p>Generates a Paystack Checkout URL. The user completes card payment on the
 * Paystack-hosted page; Paystack sends a {@code charge.success} webhook to
 * {@code /api/webhooks/paystack}, which marks the payment as funded and
 * triggers execution.
 *
 * <p><strong>Status: STUB</strong> — Integration with the Paystack Transactions API
 * ({@code POST /transaction/initialize}) is not yet implemented. The adapter
 * returns a placeholder redirect URL until Paystack credentials are configured in
 * {@code payment_rail_config.config_json}.
 *
 * <p>Required config keys: {@code apiKey}, {@code baseUrl}, {@code callbackUrl}
 *
 * <p>Processor ID: {@code paystack-card}
 */
@Component
public class PaystackCardRailAdapter implements PaymentRailAdapter {

    @Override
    public String processorId() {
        return "paystack-card";
    }

    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), "Paystack Card", "card",
                "Card payments via Paystack hosted checkout.",
                "https://paystack.com/docs/api/",
                List.of(
                    ConfigFieldSchema.requiredSecret("apiKey",     "Paystack Secret Key",
                            "sk_live_... or sk_test_..."),
                    ConfigFieldSchema.requiredUrl(   "baseUrl",    "API Base URL",
                            "https://api.paystack.co"),
                    ConfigFieldSchema.optionalText(  "callbackUrl", "Callback URL",
                            "Override default post-payment redirect URL")
                ));
    }

    /**
     * Initiates a Paystack card checkout session and returns the redirect URL.
     *
     * <p><strong>Stub:</strong> returns a placeholder URL. Replace with a call to
     * {@code POST https://api.paystack.co/transaction/initialize} using the
     * {@code apiKey} from {@code processorConfig}.
     */
    @Override
    public PaymentRailResult initiate(PaymentRailRequest request) {
        String apiKey = request.processorConfig().getOrDefault("apiKey", "");
        String baseUrl = request.processorConfig().getOrDefault("baseUrl", "https://api.paystack.co");

        // TODO: call Paystack Transactions API to generate a real checkout URL
        String checkoutUrl = "https://checkout.paystack.com/placeholder/" + request.paymentReference();

        return PaymentRailResult.pendingRedirect(
                "PSK-" + request.paymentReference(),
                checkoutUrl,
                "Complete your card payment on the Paystack-secured checkout page."
        );
    }

    /**
     * Confirms that Paystack's {@code charge.success} webhook was received.
     *
     * <p><strong>Stub:</strong> returns FUNDED optimistically. Replace with a
     * query to the payment's stored webhook confirmation flag.
     */
    @Override
    public PaymentRailResult confirmFunded(String paymentReference) {
        // TODO: check webhook confirmation flag in VtpassPayment.authorizationPayload
        return PaymentRailResult.funded("PSK-" + paymentReference, null);
    }
}
