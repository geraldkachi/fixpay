package ng.fixpay.core.transfer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives Paystack transfer webhooks (transfer.success / transfer.failed / transfer.reversed).
 * Public — no JWT required; secured via HMAC-SHA512 signature check in the service layer.
 */
@RestController
@RequestMapping("/api/webhooks/paystack")
public class PaystackWebhookController {

    private final TransferService transferService;

    public PaystackWebhookController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping(value = "/transfer", consumes = "application/json")
    public ResponseEntity<Void> transfer(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String rawBody) {
        transferService.handlePaystackWebhook(signature, rawBody);
        return ResponseEntity.ok().build();
    }
}
