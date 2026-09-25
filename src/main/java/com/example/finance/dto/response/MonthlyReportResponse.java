package com.example.finance.dto.response;

import java.math.BigDecimal;

public record MonthlyReportResponse(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount
) {
}