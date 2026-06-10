package ng.fixpay.core.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ng.fixpay.core.portal.domain.SettlementAccount;
import ng.fixpay.core.portal.domain.SettlementAccountRepository;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/portal/settlement")
public class SettlementController {

    private final SettlementAccountRepository settlementRepo;
    private final TenantPortalController       portalHelper;

    public SettlementController(SettlementAccountRepository settlementRepo,
                                 TenantPortalController portalHelper) {
        this.settlementRepo = settlementRepo;
        this.portalHelper   = portalHelper;
    }

    /** GET /api/portal/settlement — get settlement account (null if not set) */
    @GetMapping
    public ResponseEntity<ApiResponse<SettlementAccount>> get(@AuthenticationPrincipal Jwt jwt) {
        Tenant tenant = portalHelper.resolveTenant(jwt);
        Optional<SettlementAccount> acct = settlementRepo.findByTenantId(tenant.getId());
        return ResponseEntity.ok(ApiResponse.ok(acct.orElse(null)));
    }

    /** PUT /api/portal/settlement — upsert settlement account */
    @PutMapping
    public ResponseEntity<ApiResponse<SettlementAccount>> upsert(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SettlementRequest req) {

        Tenant tenant = portalHelper.resolveTenant(jwt);

        SettlementAccount acct = settlementRepo.findByTenantId(tenant.getId())
                .orElseGet(() -> new SettlementAccount(
                        tenant.getId(), req.bankCode(), req.bankName(),
                        req.accountNumber(), req.accountName(),
                        req.currency() != null ? req.currency() : "NGN"));

        // Allow updating all fields; reset verified on change of account number
        boolean accountChanged = !req.accountNumber().equals(acct.getAccountNumber());
        acct.setBankCode(req.bankCode());
        acct.setBankName(req.bankName());
        acct.setAccountNumber(req.accountNumber());
        acct.setAccountName(req.accountName());

        // If account number changed, verification is no longer valid
        // (platform re-verifies before each payout)
        if (accountChanged && acct.isVerified()) {
            // Reset by creating a new entity; settlement_accounts has UNIQUE(tenant_id)
            settlementRepo.delete(acct);
            acct = new SettlementAccount(
                    tenant.getId(), req.bankCode(), req.bankName(),
                    req.accountNumber(), req.accountName(),
                    req.currency() != null ? req.currency() : "NGN");
        }

        return ResponseEntity.ok(ApiResponse.ok(settlementRepo.save(acct)));
    }

    public record SettlementRequest(
            @NotBlank @Size(max = 10)  String bankCode,
            @NotBlank @Size(max = 120) String bankName,
            @NotBlank @Pattern(regexp = "\\d{10}", message = "Must be a 10-digit account number")
                                       String accountNumber,
            @NotBlank @Size(max = 255) String accountName,
            String currency
    ) {}
}
