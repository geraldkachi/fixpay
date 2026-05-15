package ng.fixpay.core.payment.rail.adapters;

import ng.fixpay.shared.payment.ConfigFieldSchema;
import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Paystack USSD rail.
 *
 * <p>Generates a USSD shortcode via the Paystack Charge API
 * ({@code POST /charge} with {@code type: ussd}). The user dials the code on
 * their phone; Paystack sends a {@code charge.success} webhook to
 * {@code /api/webhooks/paystack} once payment is confirmed.
 *
 * <p><strong>Status: STUB</strong> — Real Paystack Charge API integration not yet
 * implemented. The adapter currently generates a placeholder USSD code.
 *
 * <p>Required config keys: {@code apiKey}, {@code baseUrl}, {@code bankCode}
 * (Paystack USSD bank code, e.g. "737" for GTBank, "919" for UBA)
 *
 * <p>Processor ID: {@code paystack-ussd}
 */
@Component
public class PaystackUssdRailAdapter implements PaymentRailAdapter {

    @Override
    public String processorId() {
        return "paystack-ussd";
    }

    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), "Paystack USSD", "ussd",
                "USSD payments via the Paystack Charge API.",
                "https://paystack.com/docs/api/",
                List.of(
                    ConfigFieldSchema.requiredSecret("apiKey",   "Paystack Secret Key",
                            "sk_live_... or sk_test_..."),
                    ConfigFieldSchema.requiredUrl(   "baseUrl",  "API Base URL",
                            "https://api.paystack.co"),
                    ConfigFieldSchema.requiredText(  "bankCode", "USSD Bank Code",
                            "Numeric bank code used for USSD prompt (e.g. 058 for GTBank)", "058")
                ));
    }

    /**
     * Initiates a Paystack USSD charge and returns the USSD shortcode.
     *
     * <p><strong>Stub:</strong> generates a placeholder code. Replace with a call to
     * {@code POST https://api.paystack.co/charge} with {@code type: ussd} and
     * the bank code from {@code processorConfig}.
     */
    @Override
    public PaymentRailResult initiate(PaymentRailRequest request) {
        String bankCode = request.processorConfig().getOrDefault("bankCode", "737");

        // TODO: call Paystack Charge API (type=ussd) to generate a real USSD code
        String ussdCode = "*" + bankCode + "*000*" + request.amount().toPlainString() + "#";

        return PaymentRailResult.pendingUssd(
                "PSK-USSD-" + request.paymentReference(),
                ussdCode,
                "Dial " + ussdCode + " on your mobile phone to authorise payment."
        );
    }

    /**
     * Confirms that the USSD payment was completed via Paystack webhook.
     *
     * <p><strong>Stub:</strong> returns FUNDED optimistically.
     */
    @Override
    public PaymentRailResult confirmFunded(String paymentReference) {
        // TODO: check webhook confirmation flag in VtpassPayment.authorizationPayload
        return PaymentRailResult.funded("PSK-USSD-" + paymentReference, null);
    }
}
