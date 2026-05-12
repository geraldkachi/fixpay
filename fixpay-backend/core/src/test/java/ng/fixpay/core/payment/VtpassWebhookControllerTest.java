package ng.fixpay.core.payment;

import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-layer webhook tests.
 *
 * Uses @WebMvcTest so only the web and security layers are loaded —
 * no database, no Keycloak, no Redis. The service is mocked via @MockBean.
 * The actual HMAC and state-transition logic is covered in
 * VtpassPaymentServiceWebhookTest.
 */
@WebMvcTest(VtpassPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class VtpassWebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    VtpassPaymentService paymentService;

    private static final String WEBHOOK_PAYLOAD =
        "{\"paymentReference\":\"FP-VTP-TEST-001\",\"providerStatus\":\"delivered\"," +
        "\"providerCode\":\"000\",\"providerMessage\":\"Success\",\"providerRequestId\":\"PROV-001\"}";

    // ─────────────────────────────────────────────────────────────────────────
    // Happy-path: delivered status
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_validSignature_deliveredStatus_shouldReturn200WithCompletedStatus() throws Exception {
        VtpassPaymentStatusResponse stub = new VtpassPaymentStatusResponse(
            "FP-VTP-TEST-001", "completed", "delivered", "000", "Success",
            VtpassPaymentMethod.WALLET, new BigDecimal("100"),
            "PROV-001", null, null, null
        );
        when(paymentService.processWebhook(anyString(), anyString())).thenReturn(stub);

        mockMvc.perform(post("/api/payments/vtpass/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-webhook-signature", "sha256=validsig")
                .content(WEBHOOK_PAYLOAD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("completed"))
            .andExpect(jsonPath("$.data.providerStatus").value("delivered"))
            .andExpect(jsonPath("$.data.providerCode").value("000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Happy-path: processing status
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_validSignature_processingStatus_shouldReturn200WithProcessingStatus() throws Exception {
        VtpassPaymentStatusResponse stub = new VtpassPaymentStatusResponse(
            "FP-VTP-TEST-001", "processing", "processing", "099", "Processing",
            VtpassPaymentMethod.WALLET, new BigDecimal("100"),
            "PROV-001", null, null, null
        );
        when(paymentService.processWebhook(anyString(), anyString())).thenReturn(stub);

        mockMvc.perform(post("/api/payments/vtpass/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-webhook-signature", "sha256=validsig")
                .content(WEBHOOK_PAYLOAD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("processing"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security: invalid signature
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_invalidSignature_shouldReturn403() throws Exception {
        when(paymentService.processWebhook(anyString(), anyString()))
            .thenThrow(FixPayException.forbidden("Invalid webhook signature"));

        mockMvc.perform(post("/api/payments/vtpass/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-webhook-signature", "sha256=badhash")
                .content(WEBHOOK_PAYLOAD))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void webhook_missingSignature_shouldReturn403() throws Exception {
        when(paymentService.processWebhook(any(), anyString()))
            .thenThrow(FixPayException.forbidden("Invalid webhook signature"));

        mockMvc.perform(post("/api/payments/vtpass/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WEBHOOK_PAYLOAD))
            .andExpect(status().isForbidden());
    }
}
