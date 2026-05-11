package ng.fixpay.core.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VtpassClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String secretKey;

    public VtpassClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${fixpay.vtpass.base-url:https://sandbox.vtpass.com}") String baseUrl,
            @Value("${fixpay.vtpass.api-key:}") String apiKey,
            @Value("${fixpay.vtpass.secret-key:}") String secretKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public VtpassPurchaseResult purchase(
            String requestId,
            String serviceId,
            BigDecimal amount,
            String billerCustomerRef,
            String variationCode
    ) {
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            // Safe fallback for local development when credentials are absent.
            return new VtpassPurchaseResult(
                    false,
                    true,
                    "CFG",
                    "VTPass credentials are not configured",
                    "pending",
                    requestId,
                    null,
                    "{}"
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request_id", requestId);
        payload.put("serviceID", serviceId);
        payload.put("amount", amount);
        payload.put("billersCode", billerCustomerRef);
        payload.put("phone", billerCustomerRef);
        if (variationCode != null && !variationCode.isBlank()) {
            payload.put("variation_code", variationCode);
        }

        try {
            String responseBody = restClient.post()
                    .uri("/api/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .header("secret-key", secretKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody, requestId);
        } catch (Exception ex) {
            throw FixPayException.badRequest("VTPass purchase call failed: " + ex.getMessage());
        }
    }

    public VtpassPurchaseResult requery(String requestId) {
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return new VtpassPurchaseResult(
                    false,
                    true,
                    "CFG",
                    "VTPass credentials are not configured",
                    "pending",
                    requestId,
                    null,
                    "{}"
            );
        }

        Map<String, Object> payload = Map.of("request_id", requestId);

        try {
            String responseBody = restClient.post()
                    .uri("/api/requery")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .header("secret-key", secretKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody, requestId);
        } catch (Exception ex) {
            throw FixPayException.badRequest("VTPass requery call failed: " + ex.getMessage());
        }
    }

    private VtpassPurchaseResult parseResponse(String responseBody, String fallbackRequestId) {
        try {
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            String code = text(root, "code");
            String message = text(root, "response_description");
            String requestId = text(root, "requestId");
            if (requestId == null || requestId.isBlank()) {
                requestId = fallbackRequestId;
            }

            JsonNode txNode = root.path("content").path("transactions");
            String status = text(txNode, "status");
            String transactionId = text(txNode, "transactionId");

            boolean successful = "000".equals(code) && "delivered".equalsIgnoreCase(status);
            boolean pending = "000".equals(code) && (status == null || !"delivered".equalsIgnoreCase(status));
            String providerStatus = successful ? "delivered" : (pending ? "pending" : "failed");

            return new VtpassPurchaseResult(
                    successful,
                    pending,
                    code,
                    message,
                    providerStatus,
                    requestId,
                    transactionId,
                    responseBody
            );
        } catch (Exception ex) {
            throw FixPayException.badRequest("Unable to parse VTPass response: " + ex.getMessage());
        }
    }

    private String text(JsonNode node, String key) {
        JsonNode child = node == null ? null : node.get(key);
        return child == null || child.isNull() ? null : child.asText();
    }
}
