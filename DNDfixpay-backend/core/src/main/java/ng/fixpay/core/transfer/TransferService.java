package ng.fixpay.core.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.ledger.LedgerService;
import ng.fixpay.core.ledger.LedgerService.DebitResult;
import ng.fixpay.core.transfer.domain.Transfer;
import ng.fixpay.core.transfer.domain.Transfer.TransferType;
import ng.fixpay.core.transfer.domain.TransferRepository;
import ng.fixpay.core.transfer.dto.*;
import ng.fixpay.core.transfer.provider.PaystackTransferClient;
import ng.fixpay.core.transfer.provider.PaystackTransferClient.PaystackTransferResult;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    /** NIP flat fee: ₦52.50 (in NGN) */
    private static final BigDecimal BANK_TRANSFER_FEE = new BigDecimal("52.50");

    private final TransferRepository     transferRepo;
    private final UserRepository         userRepo;
    private final LedgerService          ledgerService;
    private final PaystackTransferClient paystackClient;
    private final ObjectMapper           objectMapper;

    @Value("${fixpay.paystack.secret-key:sk_test_placeholder}")
    private String paystackSecretKey;

    public TransferService(TransferRepository transferRepo,
                           UserRepository userRepo,
                           LedgerService ledgerService,
                           PaystackTransferClient paystackClient,
                           ObjectMapper objectMapper) {
        this.transferRepo   = transferRepo;
        this.userRepo       = userRepo;
        this.ledgerService  = ledgerService;
        this.paystackClient = paystackClient;
        this.objectMapper   = objectMapper;
    }

    // ─── Bank list ────────────────────────────────────────────────────────────

    public List<NipBankDto> listBanks() {
        return paystackClient.listBanks();
    }

    // ─── Name enquiry ─────────────────────────────────────────────────────────

    public NameEnquiryResponse verifyAccount(NameEnquiryRequest request) {
        List<NipBankDto> banks = paystackClient.listBanks();
        String bankName = banks.stream()
                .filter(b -> b.bankCode().equals(request.bankCode()))
                .map(NipBankDto::bankName)
                .findFirst()
                .orElse("Unknown Bank");

        return paystackClient.resolveAccount(request.accountNumber(), request.bankCode(), bankName);
    }

    // ─── Bank transfer ────────────────────────────────────────────────────────

    @Transactional
    public TransferResponse bankTransfer(Jwt jwt, BankTransferRequest request) {
        AppUser sender = resolveUser(jwt);

        BigDecimal amountNgn = BigDecimal.valueOf(request.amountKobo()).divide(BigDecimal.valueOf(100));
        BigDecimal totalDebit = amountNgn.add(BANK_TRANSFER_FEE);
        String reference = "FP-TXF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Re-verify account name at backend for security
        List<NipBankDto> banks = paystackClient.listBanks();
        String bankName = banks.stream()
                .filter(b -> b.bankCode().equals(request.bankCode()))
                .map(NipBankDto::bankName)
                .findFirst()
                .orElse("Unknown Bank");

        NameEnquiryResponse enquiry = paystackClient.resolveAccount(
                request.accountNumber(), request.bankCode(), bankName);

        // Persist the transfer record early (prevents duplicate submissions)
        Transfer transfer = new Transfer(sender.getId(), sender.getTenantId(),
                reference, TransferType.BANK, amountNgn, BANK_TRANSFER_FEE, request.narration());
        transfer.withBankRecipient(request.accountNumber(), request.bankCode(),
                bankName, enquiry.accountName());
        transferRepo.save(transfer);

        // Debit wallet (REQUIRES_NEW — can be reversed independently)
        DebitResult debit;
        try {
            debit = ledgerService.debit(sender.getId(), totalDebit, reference,
                    "Bank transfer to " + enquiry.accountName());
        } catch (FixPayException e) {
            transfer.markFailed("Insufficient balance");
            transferRepo.save(transfer);
            throw e;
        }

        // Initiate Paystack transfer
        try {
            String recipientCode = paystackClient.createRecipient(
                    enquiry.accountName(), request.accountNumber(), request.bankCode());

            PaystackTransferResult result = paystackClient.initiate(
                    recipientCode, request.amountKobo(), reference, request.narration());

            transfer.markProcessing(result.reference());
            transferRepo.save(transfer);

            log.info("[Transfer] bank transfer {} initiated, Paystack status={}", reference, result.status());

        } catch (Exception e) {
            log.error("[Transfer] Paystack call failed for {}, reversing wallet debit", reference, e);
            ledgerService.reverse(debit, reference, "Paystack transfer initiation failed");
            transfer.markFailed(e.getMessage());
            transferRepo.save(transfer);
            throw FixPayException.serviceUnavailable("Transfer could not be processed. Your wallet has not been charged.");
        }

        return toResponse(transfer);
    }

    // ─── Wallet (P2P) transfer ────────────────────────────────────────────────

    @Transactional
    public TransferResponse walletTransfer(Jwt jwt, WalletTransferRequest request) {
        AppUser sender = resolveUser(jwt);

        AppUser recipient = userRepo.findByPhone(request.recipientPhone())
                .orElseThrow(() -> FixPayException.notFound("Recipient not found on FixPay"));

        if (sender.getId().equals(recipient.getId())) {
            throw FixPayException.badRequest("Cannot transfer to your own wallet");
        }

        BigDecimal amountNgn = BigDecimal.valueOf(request.amountKobo()).divide(BigDecimal.valueOf(100));
        String reference = "FP-P2P-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Transfer transfer = new Transfer(sender.getId(), sender.getTenantId(),
                reference, TransferType.WALLET, amountNgn, BigDecimal.ZERO, request.narration());
        transfer.withWalletRecipient(recipient.getId(), request.recipientPhone());
        transferRepo.save(transfer);

        // Debit sender
        DebitResult debit;
        try {
            debit = ledgerService.debit(sender.getId(), amountNgn, reference,
                    "P2P transfer to " + recipient.getPhone());
        } catch (FixPayException e) {
            transfer.markFailed("Insufficient balance");
            transferRepo.save(transfer);
            throw e;
        }

        // Credit recipient
        try {
            ledgerService.credit(recipient.getId(), amountNgn, reference,
                    "P2P transfer from " + sender.getPhone());
        } catch (Exception e) {
            log.error("[Transfer] Credit failed for P2P {}, reversing debit", reference, e);
            ledgerService.reverse(debit, reference, "P2P credit failed");
            transfer.markFailed("Could not credit recipient wallet");
            transferRepo.save(transfer);
            throw FixPayException.serviceUnavailable("Transfer failed. Your wallet has not been charged.");
        }

        transfer.markCompleted("internal");
        transferRepo.save(transfer);

        log.info("[Transfer] P2P transfer {} completed: {} → {}",
                reference, sender.getPhone(), recipient.getPhone());

        return toResponse(transfer);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser resolveUser(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        return userRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User not found"));
    }

    private TransferResponse toResponse(Transfer t) {
        String recipientName = t.getTransferType() == TransferType.BANK
                ? t.getRecipientAccountName()
                : t.getRecipientPhone();
        String recipientBank = t.getTransferType() == TransferType.BANK
                ? t.getRecipientBankName()
                : "FixPay Wallet";

        return new TransferResponse(
                t.getId(), t.getReference(), t.getAmount(), t.getFee(),
                recipientName, recipientBank, t.getNarration(), t.getStatus(), t.getCreatedAt());
    }

    // ─── Paystack transfer webhook ────────────────────────────────────────────

    /**
     * Processes a Paystack {@code transfer.success / transfer.failed / transfer.reversed} event.
     * Validates the HMAC-SHA512 signature, then updates the transfer record and reverses the
     * wallet debit if the transfer did not succeed.
     */
    @Transactional
    public void handlePaystackWebhook(String signature, String rawBody) {
        if (!verifyPaystackSignature(signature, rawBody)) {
            log.warn("[Transfer/Webhook] Invalid Paystack signature — ignoring");
            throw FixPayException.unauthorized("Invalid webhook signature");
        }

        PaystackWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);
        } catch (Exception e) {
            log.warn("[Transfer/Webhook] Failed to parse payload: {}", e.getMessage());
            return; // acknowledge but ignore unparseable events
        }

        String event     = payload.event();
        String reference = payload.data() != null ? payload.data().reference() : null;
        if (reference == null || !reference.startsWith("FP-TXF-")) {
            log.debug("[Transfer/Webhook] Ignoring non-transfer event: {}", event);
            return; // not our transfer reference
        }

        Transfer transfer = transferRepo.findByReference(reference).orElse(null);
        if (transfer == null) {
            log.warn("[Transfer/Webhook] No transfer found for reference {}", reference);
            return;
        }

        // Idempotency guard — already in terminal status
        String status = transfer.getStatus();
        if ("completed".equals(status) || "reversed".equals(status)) {
            log.debug("[Transfer/Webhook] Transfer {} already terminal ({}), skipping", reference, status);
            return;
        }

        if ("transfer.success".equals(event)) {
            transfer.markCompleted(payload.data().transfer_code());
            transferRepo.save(transfer);
            log.info("[Transfer/Webhook] Transfer {} marked completed", reference);

        } else if ("transfer.failed".equals(event) || "transfer.reversed".equals(event)) {
            String reason = payload.data().reason() != null ? payload.data().reason() : event;
            transfer.markFailed(reason);
            transferRepo.save(transfer);

            // Reverse wallet debit: amount + fee
            BigDecimal totalDebit = transfer.getAmount().add(transfer.getFee());
            try {
                ledgerService.reverseByUser(transfer.getUserId(), totalDebit,
                        reference, "Bank transfer failed: " + reason);
                log.info("[Transfer/Webhook] Reversed wallet debit for failed transfer {}", reference);
            } catch (Exception e) {
                log.error("[Transfer/Webhook] Could not reverse wallet debit for transfer {} — manual action needed", reference, e);
            }
        }
    }

    private boolean verifyPaystackSignature(String signature, String rawBody) {
        if (signature == null || signature.isBlank() || rawBody == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("[Transfer/Webhook] HMAC verification error", e);
            return false;
        }
    }

    // ─── Webhook payload DTOs ─────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaystackWebhookPayload(String event, WebhookData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WebhookData(String reference, String transfer_code, String status, String reason) {}
}
