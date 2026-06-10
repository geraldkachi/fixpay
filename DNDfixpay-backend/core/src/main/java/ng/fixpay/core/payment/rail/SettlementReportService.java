package ng.fixpay.core.payment.rail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Settlement report service — aggregates {@code vtpass_payments} data
 * into settlement reports for a given tenant and date range.
 */
@Service
public class SettlementReportService {

    private static final Logger log = LoggerFactory.getLogger(SettlementReportService.class);

    private final JdbcTemplate jdbc;

    public SettlementReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Generates a settlement report summary for the given tenant and date range.
     */
    public SettlementReportResult generateReport(UUID tenantId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT COUNT(*)                                    AS tx_count,
                       COALESCE(SUM(ROUND(amount * 100)), 0)      AS total_amount_kobo,
                       COALESCE(SUM(processor_fee_kobo), 0)       AS total_fees_kobo
                  FROM vtpass_payments
                 WHERE payment_status = 'completed'
                   AND tenant_id = ?
                   AND DATE(created_at AT TIME ZONE 'Africa/Lagos') BETWEEN ? AND ?
                """;
        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {
                long txCount       = rs.getLong("tx_count");
                long amountKobo    = rs.getLong("total_amount_kobo");
                long feesKobo      = rs.getLong("total_fees_kobo");
                // Platform revenue = processor fees collected (VTPass passes these through to us)
                long platformRevKobo = feesKobo;
                return new SettlementReportResult(true, "OK", txCount, amountKobo, feesKobo, platformRevKobo);
            }, tenantId, from, to);
        } catch (Exception e) {
            log.error("[SettlementReportService] generateReport failed. tenantId={} from={} to={}",
                    tenantId, from, to, e);
            return SettlementReportResult.notImplemented();
        }
    }

    /**
     * Returns daily settlement cycles across all tenants for the given range.
     * Each cycle represents one day's settled volume aggregated platform-wide.
     */
    public List<SettlementCycle> listCycles(LocalDate from, LocalDate to) {
        String sql = """
                SELECT DATE(created_at AT TIME ZONE 'Africa/Lagos') AS cycle_date,
                       tenant_id,
                       COUNT(*)                                          AS tx_count,
                       COALESCE(SUM(ROUND(amount * 100)), 0)            AS total_amount_kobo,
                       COALESCE(SUM(processor_fee_kobo), 0)             AS total_fees_kobo
                  FROM vtpass_payments
                 WHERE payment_status = 'completed'
                   AND DATE(created_at AT TIME ZONE 'Africa/Lagos') BETWEEN ? AND ?
                 GROUP BY cycle_date, tenant_id
                 ORDER BY cycle_date DESC, tenant_id
                """;
        try {
            return jdbc.query(sql, (rs, rowNum) -> new SettlementCycle(
                    rs.getDate("cycle_date").toLocalDate(),
                    UUID.fromString(rs.getString("tenant_id")),
                    rs.getLong("tx_count"),
                    rs.getLong("total_amount_kobo"),
                    rs.getLong("total_fees_kobo"),
                    rs.getLong("total_fees_kobo")  // platformRevenue = fees collected
            ), from, to);
        } catch (Exception e) {
            log.error("[SettlementReportService] listCycles failed. from={} to={}", from, to, e);
            return List.of();
        }
    }

    // ─── Result DTOs ──────────────────────────────────────────────────────────

    public record SettlementReportResult(
            boolean implemented,
            String  message,
            long    totalTransactions,
            long    totalAmountKobo,
            long    totalProcessorFeesKobo,
            long    platformRevenueKobo
    ) {
        static SettlementReportResult notImplemented() {
            return new SettlementReportResult(false,
                    "Settlement data unavailable.",
                    0L, 0L, 0L, 0L);
        }
    }

    public record SettlementCycle(
            LocalDate cycleDate,
            UUID      tenantId,
            long      totalTransactions,
            long      totalAmountKobo,
            long      totalProcessorFeesKobo,
            long      platformRevenueKobo
    ) {}
}
