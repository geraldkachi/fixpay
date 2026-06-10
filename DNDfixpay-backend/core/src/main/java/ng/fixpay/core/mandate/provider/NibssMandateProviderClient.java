package ng.fixpay.core.mandate.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.mandate.dto.CreateMandateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NibssMandateProviderClient implements MandateProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String secretKey;
    private final boolean enabled;

    public NibssMandateProviderClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${fixpay.mandate.base-url:https://sandbox.nibss.local}") String baseUrl,
            @Value("${fixpay.mandate.api-key:}") String apiKey,
            @Value("${fixpay.mandate.secret-key:}") String secretKey,
            @Value("${fixpay.mandate.enabled:false}") boolean enabled
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.enabled = enabled;
    }

    @Override
    public MandateProviderResult createMandate(String mandateReference, CreateMandateRequest request) {
        if (!enabled || apiKey.isBlank() || secretKey.isBlank()) {
            return new MandateProviderResult(
                    "pending",
                    "Mandate provider not configured; remaining in pending state",
                    null,
                    "CFG",
                    "{}"
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mandateReference", mandateReference);
        payload.put("bankCode", request.bankCode());
        payload.put("accountNumber", request.accountNumber());
        payload.put("maxAmount", request.maxAmount());
        payload.put("startDate", request.startDate());
        payload.put("endDate", request.endDate());

        try {
            String response = restClient.post()
                    .uri("/api/mandates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .header("secret-key", secretKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (Exception ex) {
            return new MandateProviderResult("failed", "Mandate provider create call failed", null, "ERR", ex.getMessage());
        }
    }

    @Override
    public MandateProviderResult syncMandateStatus(String mandateReference, String providerReference) {
        if (!enabled || apiKey.isBlank() || secretKey.isBlank()) {
            return new MandateProviderResult(
                    "pending",
                    "Mandate provider not configured; keeping last known status",
                    providerReference,
                    "CFG",
                    "{}"
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mandateReference", mandateReference);
        payload.put("providerReference", providerReference);

        try {
            String response = restClient.post()
                    .uri("/api/mandates/requery")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .header("secret-key", secretKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (Exception ex) {
            return new MandateProviderResult("pending", "Mandate provider sync failed; retry later", providerReference, "ERR", ex.getMessage());
        }
    }

    private MandateProviderResult parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            String code = text(root, "code");
            String message = text(root, "message");
            JsonNode data = root.path("data");
            String providerReference = text(data, "providerReference");
            String status = text(data, "status");

            if (status == null || status.isBlank()) {
                status = "000".equals(code) ? "active" : "pending";
            }
            if (message == null || message.isBlank()) {
                message = "000".equals(code) ? "Mandate synced" : "Mandate status pending";
            }

            return new MandateProviderResult(normalizeStatus(status), message, providerReference, code, responseBody);
        } catch (Exception ex) {
            return new MandateProviderResult("pending", "Unable to parse mandate provider response", null, "PARSE", responseBody);
        }
    }

    private String normalizeStatus(String status) {
        String v = status.toLowerCase();
        return switch (v) {
            case "active", "pending", "suspended", "revoked", "expired", "failed" -> v;
            case "success", "approved", "completed" -> "active";
            case "error", "declined" -> "failed";
            default -> "pending";
        };
    }

    private String text(JsonNode node, String key) {
        JsonNode child = node == null ? null : node.get(key);
        return child == null || child.isNull() ? null : child.asText();
    }
}
