package com.example.finance.dto.response;

import java.math.BigDecimal;

public record MonthlyTrendResponse(
        String monthKey,
        String monthLabel,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netFlow
) {
}
