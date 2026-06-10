package ng.fixpay.core.payment.rail.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessorFeeScheduleRepository extends JpaRepository<ProcessorFeeSchedule, UUID> {

    /**
     * Finds the active fee schedule row for a rail config on a given date.
     * Returns at most one row (highest precedence = most specific effective_from).
     */
    @Query("""
            SELECT f FROM ProcessorFeeSchedule f
            WHERE f.railConfig.id = :railConfigId
              AND f.effectiveFrom <= :onDate
              AND (f.effectiveTo IS NULL OR f.effectiveTo >= :onDate)
            ORDER BY f.effectiveFrom DESC
            """)
    List<ProcessorFeeSchedule> findActive(@Param("railConfigId") UUID railConfigId,
                                           @Param("onDate") LocalDate onDate);

    /** Returns the first active row for a config on today's date, or empty if none. */
    default Optional<ProcessorFeeSchedule> findActiveToday(UUID railConfigId) {
        List<ProcessorFeeSchedule> rows = findActive(railConfigId, LocalDate.now());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Returns all fee schedule rows for a rail config, ordered by effective_from DESC. */
    List<ProcessorFeeSchedule> findByRailConfigIdOrderByEffectiveFromDesc(UUID railConfigId);
}
