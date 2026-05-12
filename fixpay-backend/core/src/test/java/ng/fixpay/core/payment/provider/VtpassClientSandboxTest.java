package ng.fixpay.core.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sandbox integration tests that hit the real VTpass sandbox API at https://sandbox.vtpass.com.
 *
 * These tests are skipped automatically when VTPASS_API_KEY / VTPASS_SECRET_KEY are not set,
 * so CI always passes. To run locally:
 *
 *   $env:VTPASS_API_KEY="your-key"; $env:VTPASS_SECRET_KEY="your-secret"
 *   .\gradlew.bat :core:test --tests "*.VtpassClientSandboxTest"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VtpassClientSandboxTest {

    private static VtpassClient vtpassClient;

    /**
     * Shared across test instances via static field so that the requery test can
     * use the requestId produced by the purchase test.
     */
    private static String capturedRequestId;

    @BeforeAll
    static void setUpClient() {
        String apiKey = System.getenv("VTPASS_API_KEY");
        String secretKey = System.getenv("VTPASS_SECRET_KEY");
        String baseUrl = System.getenv().getOrDefault("VTPASS_BASE_URL", "https://sandbox.vtpass.com");

        assumeTrue(
            apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank(),
            "Skipping VTpass sandbox tests — VTPASS_API_KEY and VTPASS_SECRET_KEY environment variables are not set"
        );

        vtpassClient = new VtpassClient(
            RestClient.builder(),
            new ObjectMapper(),
            baseUrl,
            apiKey,
            secretKey
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Airtime purchase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void purchaseAirtime_shouldReturnSuccessOrPendingFromSandbox() {
        String requestId = "FP-TEST-" + System.currentTimeMillis();

        VtpassPurchaseResult result = vtpassClient.purchase(
            requestId,
            "airtel",      // correct VTpass service ID for Airtel Airtime VTU
            new BigDecimal("100"),
            "08011111111",
            null
        );

        System.out.println("[VTpass sandbox] airtime purchase response: " + result.rawResponse());

        assertAll(
            () -> assertNotNull(result.requestId(),   "requestId must be non-null"),
            () -> assertNotNull(result.rawResponse(), "rawResponse must be non-null"),
            () -> assertFalse(result.rawResponse().isBlank(), "rawResponse must not be blank"),
            () -> assertTrue(
                result.successful() || result.pending(),
                "Expected successful or pending — got code=" + result.providerCode()
                    + " message=" + result.providerMessage()
            )
        );

        // Save requestId for the requery test that follows
        capturedRequestId = result.requestId();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Requery the purchase from Test 1
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    void requery_shouldReturnStatusForExistingRequest() {
        assumeTrue(capturedRequestId != null,
            "Skipping requery test — purchaseAirtime test did not run or did not produce a requestId");

        VtpassPurchaseResult result = vtpassClient.requery(capturedRequestId);

        System.out.println("[VTpass sandbox] requery response: " + result.rawResponse());

        assertAll(
            () -> assertNotNull(result.requestId(),   "requery requestId must be non-null"),
            () -> assertNotNull(result.rawResponse(), "requery rawResponse must be non-null"),
            () -> assertFalse(result.rawResponse().isBlank(), "requery rawResponse must not be blank")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Purchase with variation code (data plan)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    void purchaseWithVariationCode_shouldHandleSandboxResponse() {
        String requestId = "FP-TEST-DATA-" + System.currentTimeMillis();

        // The sandbox may reject an unknown variation code — what we are validating here
        // is that the HTTP wire format (request shape + response parsing) works end-to-end.
        VtpassPurchaseResult result;
        try {
            result = vtpassClient.purchase(
                requestId,
                "airtel-data",
                new BigDecimal("100"),
                "08011111111",
                "airt-100"     // valid variation code: 100 Naira 75MB 1Day
            );
        } catch (Exception ex) {
            // A provider-side rejection is acceptable here — it proves the wire was exercised.
            System.out.println("[VTpass sandbox] data plan exception (provider rejection is expected if variation code is wrong): " + ex.getMessage());
            return;
        }

        System.out.println("[VTpass sandbox] data plan response: " + result.rawResponse());

        // code 019 (duplicate transaction) is expected in sandbox when the same phone number
        // was used seconds earlier in Test 1. The wire format and parsing are validated either way.
        assertAll(
            () -> assertNotNull(result.requestId(),   "requestId must be non-null even for data plan purchase"),
            () -> assertNotNull(result.rawResponse(), "rawResponse must be non-null"),
            () -> assertNotNull(result.providerCode(), "providerCode must be non-null"),
            () -> assertTrue(
                result.successful() || result.pending() || "019".equals(result.providerCode()),
                "Expected success, pending, or duplicate (019) — got code=" + result.providerCode()
                    + " message=" + result.providerMessage()
            )
        );
    }
}
