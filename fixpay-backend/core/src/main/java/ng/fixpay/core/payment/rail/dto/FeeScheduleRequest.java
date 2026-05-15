package ng.fixpay.core.payment.rail.dto;

import ng.fixpay.core.payment.rail.domain.ProcessorFeeSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or updating a {@link ProcessorFeeSchedule} row
 * on a {@link ng.fixpay.core.payment.rail.domain.PaymentRailConfig}.
 */
public record FeeScheduleRequest(

        /** Fee type: FIXED, PERCENTAGE, or TIERED. */
        ProcessorFeeSchedule.FeeType feeType,

        /** Fixed component in kobo (e.g. 5000 = ₦50). */
        long fixedFeeKobo,

        /** Percentage as a decimal (e.g. 0.015000 = 1.5%). */
        BigDecimal percentageFee,

        /** Maximum fee cap in kobo. {@code null} = no ceiling. */
        Long capKobo,

        /** Minimum fee floor in kobo. */
        long minFeeKobo,

        /** Inclusive start date. Defaults to today if not provided. */
        LocalDate effectiveFrom,

        /** Inclusive end date. {@code null} = indefinitely active. */
        LocalDate effectiveTo
) {}
