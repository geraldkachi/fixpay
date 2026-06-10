package ng.fixpay.core.dispute;

import ng.fixpay.core.dispute.domain.Dispute;
import ng.fixpay.core.dispute.domain.Dispute.Category;
import ng.fixpay.core.dispute.domain.DisputeRepository;
import ng.fixpay.core.dispute.dto.DisputeResponse;
import ng.fixpay.core.dispute.dto.RaiseDisputeRequest;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.transfer.domain.Transfer;
import ng.fixpay.core.transfer.domain.TransferRepository;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DisputeService {

    private final DisputeRepository      disputeRepo;
    private final UserRepository         userRepo;
    private final VtpassPaymentRepository vtpassRepo;
    private final TransferRepository     transferRepo;

    public DisputeService(DisputeRepository disputeRepo,
                          UserRepository userRepo,
                          VtpassPaymentRepository vtpassRepo,
                          TransferRepository transferRepo) {
        this.disputeRepo = disputeRepo;
        this.userRepo    = userRepo;
        this.vtpassRepo  = vtpassRepo;
        this.transferRepo = transferRepo;
    }

    // ─── List user disputes ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DisputeResponse> listDisputes(Jwt jwt) {
        AppUser user = resolveUser(jwt);
        return disputeRepo.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(DisputeResponse::from)
                .toList();
    }

    // ─── Get single dispute ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(Jwt jwt, UUID disputeId) {
        AppUser user = resolveUser(jwt);
        Dispute dispute = disputeRepo.findByIdAndUserId(disputeId, user.getId())
                .orElseThrow(() -> FixPayException.notFound("Dispute"));
        return DisputeResponse.from(dispute);
    }

    // ─── Raise a new dispute ──────────────────────────────────────────────────

    @Transactional
    public DisputeResponse raise(Jwt jwt, RaiseDisputeRequest request) {
        AppUser user = resolveUser(jwt);

        Category category;
        try {
            category = Category.valueOf(request.category());
        } catch (IllegalArgumentException e) {
            throw FixPayException.badRequest("Invalid category. Use: WRONG_AMOUNT, NOT_RECEIVED, DOUBLE_CHARGE, UNAUTHORIZED, OTHER");
        }

        // Resolve transaction snapshot from either VTpass payment or transfer
        String  txDescription = null;
        BigDecimal txAmount   = null;
        Instant txDate        = null;

        Optional<VtpassPayment> vtpassOpt = vtpassRepo.findByPaymentReference(request.transactionReference());
        if (vtpassOpt.isPresent()) {
            VtpassPayment p = vtpassOpt.get();
            if (!p.getUserId().equals(user.getId())) {
                throw FixPayException.forbidden("Transaction does not belong to your account");
            }
            txDescription = p.getServiceId() + " payment";
            txAmount      = p.getAmount();
            txDate        = p.getCreatedAt();
        } else {
            Optional<Transfer> transferOpt = transferRepo.findByReference(request.transactionReference());
            if (transferOpt.isPresent()) {
                Transfer t = transferOpt.get();
                if (!t.getUserId().equals(user.getId())) {
                    throw FixPayException.forbidden("Transaction does not belong to your account");
                }
                txDescription = "Transfer to " + (t.getRecipientAccountName() != null
                        ? t.getRecipientAccountName() : t.getRecipientPhone());
                txAmount      = t.getAmount();
                txDate        = t.getCreatedAt();
            }
            // If reference not found in either table, still allow the dispute (user may have the wrong ref)
        }

        Dispute dispute = new Dispute(user.getId(), user.getTenantId(),
                request.transactionReference(), txDescription, txAmount, txDate,
                category, request.description());

        disputeRepo.save(dispute);
        return DisputeResponse.from(dispute);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser resolveUser(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        return userRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User not found"));
    }
}
