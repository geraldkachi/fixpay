package ng.fixpay.core.mandate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMandateRequest(
        @NotBlank(message = "bankCode is required")
        @Pattern(regexp = "^\\d{3,6}$", message = "bankCode format is invalid")
        String bankCode,

        @NotBlank(message = "accountNumber is required")
        @Pattern(regexp = "^\\d{10}$", message = "accountNumber must be 10 digits")
        String accountNumber,

        @NotNull(message = "maxAmount is required")
        @DecimalMin(value = "1.00", message = "maxAmount must be greater than 0")
        BigDecimal maxAmount,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        LocalDate endDate
) {}
