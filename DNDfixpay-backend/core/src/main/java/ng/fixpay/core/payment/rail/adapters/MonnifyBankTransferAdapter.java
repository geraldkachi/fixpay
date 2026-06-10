package ng.fixpay.core.payment.rail.adapters;

import ng.fixpay.shared.payment.ConfigFieldSchema;
import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monnify bank transfer rail.
 *
 * <p>Reserves a Monnify virtual account number for the transaction. The user
 * transfers the exact amount to that account; Monnify sends a
 * {@code SUCCESSFUL_TRANSACTION} webhook to {@code /api/webhooks/monnify},
 * which marks the payment as funded and triggers execution.
 *
 * <p><strong>Status: STUB</strong> — Real Monnify Reserved Account API integration
 * not yet implemented. Returns placeholder account details.
 *
 * <p>Required config keys: {@code apiKey}, {@code secretKey}, {@code contractCode},
 * {@code baseUrl} (default: {@code https://api.monnify.com})
 *
 * <p>Processor ID: {@code monnify-transfer}
 */
@Component
public class MonnifyBankTransferAdapter implements PaymentRailAdapter {

    @Override
    public String processorId() {
        return "monnify-transfer";
    }

    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), "Monnify Bank Transfer", "bank_transfer",
                "Virtual account bank transfers via Monnify.",
                "https://docs.monnify.com/",
                List.of(
                    ConfigFieldSchema.requiredSecret("apiKey",       "API Key",           ""),
                    ConfigFieldSchema.requiredSecret("secretKey",    "Secret Key",        ""),
                    ConfigFieldSchema.requiredText(  "contractCode", "Contract Code",
                            "Merchant contract code from Monnify dashboard", ""),
                    ConfigFieldSchema.requiredUrl(   "baseUrl",      "API Base URL",
                            "https://api.monnify.com")
                ));
    }

    /**
     * Reserves a Monnify virtual account and returns the account details.
     *
     * <p><strong>Stub:</strong> returns placeholder account information. Replace
     * with a call to {@code POST https://api.monnify.com/api/v2/bank-transfer/reserved-accounts}
     * or the dynamic virtual account endpoint.
     */
    @Override
    public PaymentRailResult initiate(PaymentRailRequest request) {
        // TODO: call Monnify Reserved Accounts API to create a dynamic virtual account
        String virtualAccountNumber = "0000000000"; // placeholder
        String bankName = "Monnify Test Bank";

        return PaymentRailResult.pendingTransfer(
                "MNF-" + request.paymentReference(),
                virtualAccountNumber,
                bankName,
                "Transfer exactly ₦" + request.amount().toPlainString() +
                " to " + bankName + " account " + virtualAccountNumber +
                " to complete payment. Account expires in 30 minutes."
        );
    }

    /**
     * Confirms receipt of the bank transfer via Monnify webhook.
     *
     * <p><strong>Stub:</strong> returns FUNDED optimistically.
     */
    @Override
    public PaymentRailResult confirmFunded(String paymentReference) {
        // TODO: check webhook confirmation flag in VtpassPayment.authorizationPayload
        return PaymentRailResult.funded("MNF-" + paymentReference, null);
    }
}
