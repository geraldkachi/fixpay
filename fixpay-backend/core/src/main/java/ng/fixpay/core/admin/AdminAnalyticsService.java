package ng.fixpay.core.admin;

import ng.fixpay.core.admin.dto.AnalyticsResponse;
import ng.fixpay.core.admin.dto.AnalyticsResponse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AdminAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsService.class);
    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);

    private final JdbcTemplate jdbc;

    public AdminAnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AnalyticsResponse getAnalytics() {
        return new AnalyticsResponse(
                dailyVolume(),
                revenueTrend(),
                processorBreakdown(),
                kpis()
        );
    }

    // ─── Daily volume: last 30 days ───────────────────────────────────────────

    private List<DailyVolumeDto> dailyVolume() {
        String sql = """
                SELECT DATE(created_at AT TIME ZONE 'Africa/Lagos') AS day,
                       COUNT(*) AS tx_count,
                       COALESCE(SUM(amount), 0) AS total_volume
                  FROM vtpass_payments
                 WHERE payment_status = 'completed'
                   AND created_at >= NOW() - INTERVAL '30 days'
                 GROUP BY day
                 ORDER BY day
                """;
        try {
            return jdbc.query(sql, (rs, rowNum) -> {
                String label = rs.getDate("day").toLocalDate().format(DAY_FMT);
                return new DailyVolumeDto(label, rs.getLong("tx_count"), rs.getDouble("total_volume"));
            });
        } catch (Exception e) {
            log.warn("[Analytics] dailyVolume query failed", e);
            return List.of();
        }
    }

    // ─── Revenue trend: last 6 months ────────────────────────────────────────

    private List<RevenueTrendDto> revenueTrend() {
        String sql = """
                SELECT DATE_TRUNC('month', created_at AT TIME ZONE 'Africa/Lagos') AS month_start,
                       COALESCE(SUM(processor_fee_kobo) / 100.0, 0) AS total_fee_ngn
                  FROM vtpass_payments
                 WHERE payment_status = 'completed'
                   AND created_at >= NOW() - INTERVAL '6 months'
                 GROUP BY month_start
                 ORDER BY month_start
                """;
        try {
            return jdbc.query(sql, (rs, rowNum) -> {
                // Return all fees as 'fixed'; 'percentage' is 0 until fee-type split is tracked per row
                String monthLabel = rs.getTimestamp("month_start")
                        .toLocalDateTime().toLocalDate().withDayOfMonth(1).format(MONTH_FMT);
                double feeNgn = rs.getDouble("total_fee_ngn");
                return new RevenueTrendDto(monthLabel, feeNgn, 0.0);
            });
        } catch (Exception e) {
            log.warn("[Analytics] revenueTrend query failed", e);
            return List.of();
        }
    }

    // ─── Processor breakdown by service category ──────────────────────────────

    private List<ProcessorBreakdownDto> processorBreakdown() {
        String sql = """
                SELECT service_id,
                       COUNT(*) AS cnt,
                       ROUND(COUNT(*) * 100.0 / NULLIF(SUM(COUNT(*)) OVER (), 0), 1) AS pct
                  FROM vtpass_payments
                 WHERE payment_status = 'completed'
                 GROUP BY service_id
                 ORDER BY cnt DESC
                """;
        try {
            return jdbc.query(sql, (rs, rowNum) -> {
                String name = capitalise(rs.getString("service_id"));
                return new ProcessorBreakdownDto(name, rs.getLong("cnt"), rs.getDouble("pct"));
            });
        } catch (Exception e) {
            log.warn("[Analytics] processorBreakdown query failed", e);
            return List.of();
        }
    }

    // ─── KPIs ─────────────────────────────────────────────────────────────────

    private KpiDto kpis() {
        String sql = """
                SELECT COUNT(*)                                                         AS total,
                       COALESCE(SUM(amount) FILTER (WHERE payment_status = 'completed'), 0) AS total_vol,
                       COALESCE(SUM(processor_fee_kobo) FILTER (WHERE payment_status = 'completed') / 100.0, 0) AS revenue,
                       COALESCE(AVG(amount) FILTER (WHERE payment_status = 'completed'), 0)  AS avg_val,
                       ROUND(
                           COUNT(*) FILTER (WHERE payment_status = 'completed') * 100.0
                           / NULLIF(COUNT(*), 0), 2
                       ) AS success_rate
                  FROM vtpass_payments
                """;
        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> new KpiDto(
                    rs.getLong("total"),
                    rs.getDouble("total_vol"),
                    rs.getDouble("revenue"),
                    rs.getDouble("avg_val"),
                    rs.getDouble("success_rate")
            ));
        } catch (Exception e) {
            log.warn("[Analytics] kpis query failed", e);
            return new KpiDto(0, 0, 0, 0, 0);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String capitalise(String s) {
        if (s == null || s.isBlank()) return "Other";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).replace('_', ' ');
    }
}
