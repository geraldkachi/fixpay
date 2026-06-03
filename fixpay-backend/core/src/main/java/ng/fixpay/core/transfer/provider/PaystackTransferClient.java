package ng.fixpay.core.transfer.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.transfer.dto.NameEnquiryResponse;
import ng.fixpay.core.transfer.dto.NipBankDto;
import ng.fixpay.shared.exception.FixPayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP client for Paystack Transfers API.
 *
 * <p>Handles bank list retrieval, NIP account resolution, recipient creation,
 * and transfer initiation. All amounts sent to Paystack are in <strong>kobo</strong>
 * (smallest NGN unit).
 *
 * <p>API docs: https://paystack.com/docs/transfers/
 */
@Component
public class PaystackTransferClient {

    private static final Logger log = LoggerFactory.getLogger(PaystackTransferClient.class);

    private final String   secretKey;
    private final String   baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public PaystackTransferClient(
            @Value("${fixpay.paystack.secret-key:sk_test_placeholder}") String secretKey,
            @Value("${fixpay.paystack.base-url:https://api.paystack.co}") String baseUrl,
            ObjectMapper mapper) {
        this.secretKey = secretKey;
        this.baseUrl   = baseUrl;
        this.http      = HttpClient.newHttpClient();
        this.mapper    = mapper;
    }

    // ─── Bank list ────────────────────────────────────────────────────────────

    /**
     * Returns NUBAN-enabled banks in Nigeria, cached for 24 hours.
     */
    @Cacheable(value = "paystack:banks")
    public List<NipBankDto> listBanks() {
        try {
            HttpRequest req = get("/bank?currency=NGN&type=nuban&per_page=200&use_cursor=false");
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            PagedResponse<BankData> body = mapper.readValue(res.body(),
                    mapper.getTypeFactory().constructParametricType(PagedResponse.class, BankData.class));
            if (!body.status) {
                log.warn("[Paystack] Bank list failed: {}", body.message);
                throw FixPayException.serviceUnavailable("Could not retrieve bank list");
            }
            return body.data.stream()
                    .map(b -> new NipBankDto(b.code, b.name))
                    .toList();
        } catch (FixPayException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Paystack] listBanks error", e);
            throw FixPayException.serviceUnavailable("Bank list service unavailable");
        }
    }

    // ─── Name enquiry ─────────────────────────────────────────────────────────

    /**
     * Resolves a NUBAN account number to the account holder's name.
     */
    public NameEnquiryResponse resolveAccount(String accountNumber, String bankCode, String bankName) {
        try {
            HttpRequest req = get("/bank/resolve?account_number=" + accountNumber + "&bank_code=" + bankCode);
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            SingleResponse<ResolveData> body = mapper.readValue(res.body(),
                    mapper.getTypeFactory().constructParametricType(SingleResponse.class, ResolveData.class));
            if (!body.status) {
                log.warn("[Paystack] Account resolve failed for {}/{}: {}", accountNumber, bankCode, body.message);
                throw FixPayException.badRequest("Account not found — please verify the account number and bank");
            }
            return new NameEnquiryResponse(
                    body.data.account_number,
                    body.data.account_name,
                    bankCode,
                    bankName,
                    UUID.randomUUID().toString()   // NIP session ID (Paystack doesn't expose one)
            );
        } catch (FixPayException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Paystack] resolveAccount error", e);
            throw FixPayException.serviceUnavailable("Account verification service unavailable");
        }
    }

    // ─── Recipient ────────────────────────────────────────────────────────────

    /**
     * Creates a Paystack transfer recipient for the given bank account.
     *
     * @return the Paystack recipient_code (e.g. "RCP_...")
     */
    public String createRecipient(String accountName, String accountNumber, String bankCode) {
        try {
            Map<String, Object> body = Map.of(
                    "type", "nuban",
                    "name", accountName,
                    "account_number", accountNumber,
                    "bank_code", bankCode,
                    "currency", "NGN"
            );
            HttpRequest req = post("/transferrecipient", body);
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            SingleResponse<RecipientData> parsed = mapper.readValue(res.body(),
                    mapper.getTypeFactory().constructParametricType(SingleResponse.class, RecipientData.class));
            if (!parsed.status || parsed.data == null || parsed.data.recipient_code == null) {
                log.warn("[Paystack] Create recipient failed: {}", parsed.message);
                throw FixPayException.serviceUnavailable("Could not register transfer recipient");
            }
            return parsed.data.recipient_code;
        } catch (FixPayException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Paystack] createRecipient error", e);
            throw FixPayException.serviceUnavailable("Transfer service unavailable");
        }
    }

    // ─── Transfer ─────────────────────────────────────────────────────────────

    /**
     * Initiates a transfer from the FixPay Paystack balance to the recipient.
     *
     * @param recipientCode Paystack recipient_code from {@link #createRecipient}
     * @param amountKobo    amount in kobo
     * @param reference     unique FixPay reference (idempotency key for Paystack)
     * @param narration     human-readable reason shown on beneficiary's statement
     * @return Paystack transfer reference
     */
    public PaystackTransferResult initiate(String recipientCode, long amountKobo,
                                           String reference, String narration) {
        try {
            Map<String, Object> body = Map.of(
                    "source", "balance",
                    "amount", amountKobo,
                    "recipient", recipientCode,
                    "reason", narration != null ? narration : "FixPay Transfer",
                    "reference", reference
            );
            HttpRequest req = post("/transfer", body);
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            SingleResponse<TransferData> parsed = mapper.readValue(res.body(),
                    mapper.getTypeFactory().constructParametricType(SingleResponse.class, TransferData.class));
            if (!parsed.status || parsed.data == null) {
                log.warn("[Paystack] Transfer initiation failed: {}", parsed.message);
                throw FixPayException.serviceUnavailable("Transfer initiation failed: " + parsed.message);
            }
            return new PaystackTransferResult(parsed.data.reference, parsed.data.status);
        } catch (FixPayException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Paystack] initiate transfer error", e);
            throw FixPayException.serviceUnavailable("Transfer service unavailable");
        }
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/json")
                .GET()
                .build();
    }

    private HttpRequest post(String path, Object body) throws Exception {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
    }

    // ─── Response shapes ─────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PagedResponse<T>(boolean status, String message, List<T> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SingleResponse<T>(boolean status, String message, T data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BankData(String name, String code) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResolveData(String account_number, String account_name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RecipientData(String recipient_code) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TransferData(String reference, String status) {}

    // ─── Result ───────────────────────────────────────────────────────────────

    public record PaystackTransferResult(String reference, String status) {
        public boolean isPending() {
            return "pending".equalsIgnoreCase(status) || "otp".equalsIgnoreCase(status);
        }
        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }
    }
}
