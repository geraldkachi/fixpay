package ng.fixpay.core.admin;

import ng.fixpay.core.payment.rail.SettlementReportService;
import ng.fixpay.core.payment.rail.SettlementReportService.SettlementCycle;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Settlement cycles endpoint for the admin dashboard.
 * Requires {@code PLATFORM_ADMIN} authority (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/settlement")
public class AdminSettlementController {

    private final SettlementReportService settlementService;

    public AdminSettlementController(SettlementReportService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * Returns daily settlement cycles for the given date range (defaults: last 30 days).
     * Shape: {@code { cycles: SettlementCycle[] }}
     */
    @GetMapping("/cycles")
    public ApiResponse<Map<String, Object>> cycles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(29);

        List<SettlementCycle> cycles = settlementService.listCycles(effectiveFrom, effectiveTo);
        return ApiResponse.ok(Map.of("cycles", cycles));
    }
}
