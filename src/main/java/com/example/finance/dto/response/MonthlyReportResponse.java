package com.example.finance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount,
        Double savingsRate,
        List<CategoryExpenseResponse> categories
) {
    public MonthlyReportResponse(
            int year,
            int month,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netAmount) {
        this(year, month, totalIncome, totalExpense, netAmount, 0.0, List.of());
    }
}