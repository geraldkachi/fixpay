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
public class NinVerificationProvider {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public NinVerificationProvider(
            RestTemplateBuilder builder,
            NinVerificationProperties properties) {
        this.restTemplate = builder.build();
        this.baseUrl = properties.getBaseUrl();
        this.apiKey = properties.getApiKey();
    }

    public IdentityVerificationResult verifyNin(String nin, String directorName) {
        if (!isNinValid(nin)) {
            return new IdentityVerificationResult(false, null, "Invalid NIN format");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, String> payload = Map.of(
                    "nin", nin,
                    "name", directorName
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<NinResponse> response = restTemplate.postForEntity(
                    baseUrl + "/verify/nin",
                    request,
                    NinResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                NinResponse body = response.getBody();
                return new IdentityVerificationResult(
                        body.isVerified,
                        "NIN-" + UUID.randomUUID(),
                        body.message
                );
            }
            return new IdentityVerificationResult(false, null, "NIN verification failed");
        } catch (Exception ex) {
            throw new FixPayException("NIN verification error: " + ex.getMessage(), "VERIFICATION_ERROR", 500);
        }
    }

    private boolean isNinValid(String nin) {
        return nin != null && nin.matches("^[0-9]{11}$");
    }

    public record NinResponse(
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
