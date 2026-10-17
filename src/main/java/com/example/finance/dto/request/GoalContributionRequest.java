package com.example.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GoalContributionRequest(
        @NotNull(message = "Source account ID is required")
        Long accountId,

        @NotNull(message = "Contribution amount is required")
        @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
        BigDecimal amount,

        String notes
) {
}
