package ng.fixpay.core.payment.rail.adapters;

import ng.fixpay.shared.payment.ConfigFieldSchema;
import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OPay checkout rail.
 *
 * <p>Creates an OPay payment order and redirects the user to the OPay-hosted
 * checkout page where they can pay with their OPay wallet, card, or bank account.
 * OPay sends a webhook to {@code /api/webhooks/opay} once payment is confirmed.
 *
 * <p><strong>Status: STUB</strong> — Real OPay Payment API integration not yet
 * implemented. Returns a placeholder redirect URL.
 *
 * <p>Required config keys: {@code merchantId}, {@code publicKey}, {@code privateKey},
 * {@code baseUrl} (default: {@code https://sandboxapi.opayweb.com} for sandbox)
 *
 * <p>Processor ID: {@code opay-checkout}
 */
@Component
public class OpayRailAdapter implements PaymentRailAdapter {

    @Override
    public String processorId() {
        return "opay-checkout";
    }

    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), "OPay Checkout", "other",
                "Checkout payments via OPay Cashier.",
                "https://documentation.opayweb.com/",
                List.of(
                    ConfigFieldSchema.requiredText(  "merchantId",  "Merchant ID",
                            "OPay merchant ID from the dashboard", ""),
                    ConfigFieldSchema.requiredSecret("publicKey",   "Public Key",  ""),
                    ConfigFieldSchema.requiredSecret("privateKey",  "Private Key", ""),
                    ConfigFieldSchema.requiredUrl(   "baseUrl",     "API Base URL",
                            "https://cashierapi.opayweb.com")
                ));
    }

    /**
     * Creates an OPay payment order and returns the checkout redirect URL.
     *
     * <p><strong>Stub:</strong> returns a placeholder URL. Replace with a call to
     * the OPay Payment Cashier API ({@code POST /api/v3/cashier/initialize}).
     */
    @Override
    public PaymentRailResult initiate(PaymentRailRequest request) {
        // TODO: call OPay Cashier API to create a payment order and get the checkout URL
        String checkoutUrl = "https://opayweb.com/checkout/placeholder/" + request.paymentReference();

        return PaymentRailResult.pendingRedirect(
                "OPAY-" + request.paymentReference(),
                checkoutUrl,
                "Complete your payment using OPay on the checkout page."
        );
    }

    /**
     * Confirms that the OPay payment webhook was received.
     *
     * <p><strong>Stub:</strong> returns FUNDED optimistically.
     */
    @Override
    public PaymentRailResult confirmFunded(String paymentReference) {
        // TODO: check webhook confirmation flag in VtpassPayment.authorizationPayload
        return PaymentRailResult.funded("OPAY-" + paymentReference, null);
    }
}
