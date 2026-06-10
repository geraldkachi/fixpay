package ng.fixpay.core.kyc.provider;

import ng.fixpay.shared.exception.FixPayException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class CacVerificationProvider {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public CacVerificationProvider(
            RestTemplateBuilder builder,
            CacVerificationProperties properties) {
        this.restTemplate = builder.build();
        this.baseUrl = properties.getBaseUrl();
        this.apiKey = properties.getApiKey();
    }

    public RegistrationVerificationResult verifyCac(String cacRegistrationNumber, String companyName) {
        if (!isCacValid(cacRegistrationNumber)) {
            return new RegistrationVerificationResult(false, null, "Invalid CAC registration number format");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, String> payload = Map.of(
                    "cacNumber", cacRegistrationNumber,
                    "companyName", companyName
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<CacResponse> response = restTemplate.postForEntity(
                    baseUrl + "/verify/cac",
                    request,
                    CacResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                CacResponse body = response.getBody();
                return new RegistrationVerificationResult(
                        body.isRegistered,
                        "CAC-" + UUID.randomUUID(),
                        body.message
                );
            }
            return new RegistrationVerificationResult(false, null, "CAC verification failed");
        } catch (Exception ex) {
            throw new FixPayException("CAC verification error: " + ex.getMessage(), "VERIFICATION_ERROR", 500);
        }
    }

    private boolean isCacValid(String cacNumber) {
        return cacNumber != null && cacNumber.toUpperCase().matches("^[A-Z]{2,4}[0-9]{3,10}$");
    }

    public record CacResponse(
            boolean isRegistered,
            String companyName,
            String registrationDate,
            String message
    ) {}

    public record RegistrationVerificationResult(
            boolean verified,
            String reference,
            String message
    ) {}
}
