package com.example.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank(message = "Category is required")
        String category,

        @NotNull(message = "Limit amount is required")
        @DecimalMin(value = "0.01", message = "Limit amount must be greater than zero")
        BigDecimal limitAmount,

        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        int month,

        @Min(value = 2000, message = "Year must be valid")
        int year
) {
}
