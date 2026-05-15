package ng.fixpay.core.payment.rail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Stub for the future billing/settlement report generation.
 *
 * <p>This service will aggregate {@code vtpass_payments.processor_fee_kobo} and
 * platform markup data into downloadable settlement reports for a given tenant
 * and date range. The stub is wired in now so the admin API can expose the
 * endpoint; implementation is deferred until the billing module is built.
 *
 * <p><strong>Expected output (when implemented):</strong>
 * <ul>
 *   <li>CSV or JSON report of all settled transactions in the period</li>
 *   <li>Total processor fees incurred per processor</li>
 *   <li>Platform revenue after fee deduction</li>
 *   <li>Per-tenant breakdown suitable for invoicing</li>
 * </ul>
 */
@Service
public class SettlementReportService {

    private static final Logger log = LoggerFactory.getLogger(SettlementReportService.class);

    /**
     * Generates a settlement report summary for the given tenant and date range.
     *
     * @param tenantId  the tenant to report on
     * @param from      inclusive start date
     * @param to        inclusive end date
     * @return a placeholder {@link SettlementReportResult} — not implemented yet
     */
    public SettlementReportResult generateReport(UUID tenantId, LocalDate from, LocalDate to) {
        log.warn("[SettlementReportService] generateReport() called but not yet implemented. " +
                 "tenantId={} from={} to={}", tenantId, from, to);
        return SettlementReportResult.notImplemented();
    }

    // ─── Result DTO ───────────────────────────────────────────────────────────

    public record SettlementReportResult(
            boolean implemented,
            String message,
            long totalTransactions,
            long totalAmountKobo,
            long totalProcessorFeesKobo,
            long platformRevenueKobo
    ) {
        static SettlementReportResult notImplemented() {
            return new SettlementReportResult(false,
                    "Settlement report generation is not yet implemented. " +
                    "Processor fee data is being collected in vtpass_payments.processor_fee_kobo " +
                    "and will be aggregated when the billing module is built.",
                    0L, 0L, 0L, 0L);
        }
    }
}
