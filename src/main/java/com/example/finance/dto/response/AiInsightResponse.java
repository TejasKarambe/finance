package com.example.finance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AiInsightResponse(
        int financialHealthScore,
        String healthGrade, // "A+", "A", "B", "C", "D"
        String healthSummary,
        double monthlySavingsRate,
        String topSpendingCategory,
        BigDecimal totalMonthlyBurn,
        BigDecimal recurringExpensesTotal,
        double recurringExpenseRatio,
        List<SpendingInsightItem> insights
) {
}
