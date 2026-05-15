package ng.fixpay.core.payment.rail;

import ng.fixpay.core.payment.rail.domain.ProcessorFeeSchedule;
import ng.fixpay.core.payment.rail.domain.ProcessorFeeScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Calculates the processor fee for a payment transaction.
 *
 * <h3>Formula</h3>
 * <pre>
 *   amount_kobo = amount_naira × 100
 *   raw_fee     = fixed_fee_kobo + (amount_kobo × percentage_fee)
 *   fee_kobo    = max(min_fee_kobo, min(cap_kobo, raw_fee))
 * </pre>
 *
 * <p>Returns {@code 0} if no active fee schedule exists for the config — this means
 * the fee is untracked, not that the processor is free. Admins should always configure
 * a fee schedule row for each enabled processor.
 */
@Service
public class FeeCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(FeeCalculatorService.class);

    private final ProcessorFeeScheduleRepository feeRepository;

    public FeeCalculatorService(ProcessorFeeScheduleRepository feeRepository) {
        this.feeRepository = feeRepository;
    }

    /**
     * Calculates the processor fee for a transaction amount.
     *
     * @param railConfigId the ID of the active {@link ng.fixpay.core.payment.rail.domain.PaymentRailConfig}
     * @param amountNaira  the transaction amount in Naira
     * @return the fee in kobo (rounded half-up), or {@code 0} if no schedule is configured
     */
    public long calculateFee(UUID railConfigId, BigDecimal amountNaira) {
        if (railConfigId == null || amountNaira == null) return 0L;

        return feeRepository.findActiveToday(railConfigId).map(schedule -> {
            BigDecimal amountKobo = amountNaira.multiply(BigDecimal.valueOf(100));
            BigDecimal rawFee = BigDecimal.valueOf(schedule.getFixedFeeKobo())
                    .add(amountKobo.multiply(schedule.getPercentageFee()));

            // Apply ceiling
            if (schedule.getCapKobo() != null) {
                rawFee = rawFee.min(BigDecimal.valueOf(schedule.getCapKobo()));
            }
            // Apply floor
            rawFee = rawFee.max(BigDecimal.valueOf(schedule.getMinFeeKobo()));

            long feeKobo = rawFee.setScale(0, RoundingMode.HALF_UP).longValue();
            log.debug("[FeeCalculatorService] railConfig={} amount=₦{} → fee={}k (type={})",
                    railConfigId, amountNaira, feeKobo, schedule.getFeeType());
            return feeKobo;
        }).orElseGet(() -> {
            log.debug("[FeeCalculatorService] No active fee schedule for railConfig={} — fee=0", railConfigId);
            return 0L;
        });
    }
}
