package ng.fixpay.core.kyc.provider;

import ng.fixpay.shared.exception.FixPayException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class BvnVerificationProvider {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public BvnVerificationProvider(
            RestTemplateBuilder builder,
            BvnVerificationProperties properties) {
        this.restTemplate = builder.build();
        this.baseUrl = properties.getBaseUrl();
        this.apiKey = properties.getApiKey();
    }

    public IdentityVerificationResult verifyBvn(String bvn, String directorName) {
        if (!isBvnValid(bvn)) {
            return new IdentityVerificationResult(false, null, "Invalid BVN format");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, String> payload = Map.of(
                    "bvn", bvn,
                    "name", directorName
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<BvnResponse> response = restTemplate.postForEntity(
                    baseUrl + "/verify/bvn",
                    request,
                    BvnResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                BvnResponse body = response.getBody();
                return new IdentityVerificationResult(
                        body.isVerified,
                        "BVN-" + UUID.randomUUID(),
                        body.message
                );
            }
            return new IdentityVerificationResult(false, null, "BVN verification failed");
        } catch (Exception ex) {
            throw new FixPayException("BVN verification error: " + ex.getMessage(), "VERIFICATION_ERROR", 500);
        }
    }

    private boolean isBvnValid(String bvn) {
        return bvn != null && bvn.matches("^[0-9]{11}$");
    }

    public record BvnResponse(
            boolean isVerified,
            String firstName,
            String lastName,
            String message
    ) {}

    public record IdentityVerificationResult(
            boolean verified,
            String reference,
            String message
    ) {}
}
