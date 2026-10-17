package com.example.finance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record YearlyReportResponse(
        int year,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount,
        Double savingsRate,
        List<MonthlyFlowItem> monthlyBreakdown,
        List<CategoryExpenseResponse> topCategories
) {
}
