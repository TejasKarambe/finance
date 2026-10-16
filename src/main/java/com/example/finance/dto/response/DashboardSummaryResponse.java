package com.example.finance.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netCashFlow,
        BigDecimal totalAccountBalance,
        int accountsCount,
        long totalTransactionsCount,
        Integer periodMonth,
        Integer periodYear
) {
}
