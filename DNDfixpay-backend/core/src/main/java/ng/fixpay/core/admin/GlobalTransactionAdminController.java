package ng.fixpay.core.admin;

import ng.fixpay.core.admin.dto.LedgerEntryAdminResponse;
import ng.fixpay.core.ledger.domain.LedgerEntry;
import ng.fixpay.core.ledger.domain.LedgerEntryRepository;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cross-tenant transaction ledger admin API.
 *
 * <pre>
 * GET  /api/admin/transactions                — paginated ledger (all tenants)
 * GET  /api/admin/transactions/{id}           — single entry detail
 * GET  /api/admin/transactions/correlation/{cid} — all legs of a double-entry pair
 * GET  /api/admin/transactions/reference/{ref}   — all entries by business reference
 * </pre>
 *
 * Requires {@code PLATFORM_ADMIN} or {@code FINANCE_OPS} authority.
 * (Authorization is enforced at the SecurityConfig level — /api/admin/** requires PLATFORM_ADMIN.
 * If FINANCE_OPS should also access, add that to the requestMatchers in SecurityConfig.)
 */
@RestController
@RequestMapping("/api/admin/transactions")
public class GlobalTransactionAdminController {

    private final LedgerEntryRepository ledgerRepo;

    public GlobalTransactionAdminController(LedgerEntryRepository ledgerRepo) {
        this.ledgerRepo = ledgerRepo;
    }

    /**
     * Paginated cross-tenant ledger search.
     *
     * @param tenantId   filter by tenant UUID (optional)
     * @param userId     filter by user UUID (optional)
     * @param entryType  DEBIT or CREDIT (optional)
     * @param from       ISO-8601 start timestamp (optional)
     * @param to         ISO-8601 end timestamp (optional)
     * @param reference  partial match on reference field (optional)
     */
    @GetMapping
    public ApiResponse<Page<LedgerEntryAdminResponse>> list(
            @RequestParam(required = false) UUID   tenantId,
            @RequestParam(required = false) UUID   userId,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        LedgerEntry.EntryType typeEnum = entryType == null ? null
                : parseEntryType(entryType);
        Instant fromDate = from == null ? null : Instant.parse(from);
        Instant toDate   = to   == null ? null : Instant.parse(to);

        Page<LedgerEntryAdminResponse> result = ledgerRepo
                .adminSearch(tenantId, userId, typeEnum, fromDate, toDate, reference,
                        PageRequest.of(page, Math.min(size, 200), Sort.by("createdAt").descending()))
                .map(LedgerEntryAdminResponse::from);

        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<LedgerEntryAdminResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(LedgerEntryAdminResponse.from(
                ledgerRepo.findById(id)
                        .orElseThrow(() -> FixPayException.notFound("Ledger entry " + id))));
    }

    @GetMapping("/correlation/{correlationId}")
    public ApiResponse<List<LedgerEntryAdminResponse>> byCorrelation(
            @PathVariable String correlationId) {

        List<LedgerEntryAdminResponse> entries = ledgerRepo
                .findByCorrelationIdOrderByCreatedAtAsc(correlationId)
                .stream()
                .map(LedgerEntryAdminResponse::from)
                .toList();
        return ApiResponse.ok(entries);
    }

    @GetMapping("/reference/{reference}")
    public ApiResponse<List<LedgerEntryAdminResponse>> byReference(
            @PathVariable String reference) {

        List<LedgerEntryAdminResponse> entries = ledgerRepo
                .findByReferenceOrderByCreatedAtAsc(reference)
                .stream()
                .map(LedgerEntryAdminResponse::from)
                .toList();
        return ApiResponse.ok(entries);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private LedgerEntry.EntryType parseEntryType(String value) {
        try {
            return LedgerEntry.EntryType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw FixPayException.badRequest("Invalid entryType: " + value + ". Must be DEBIT or CREDIT");
        }
    }
}
