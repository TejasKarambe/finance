package com.example.finance.dto.response;

import java.math.BigDecimal;

public record BudgetStatusResponse(
        Long budgetId,
        String category,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        Double percentageSpent,
        String status, // "NORMAL" | "WARNING" | "EXCEEDED"
        String alertMessage
) {
}
