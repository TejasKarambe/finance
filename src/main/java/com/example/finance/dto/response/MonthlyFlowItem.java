package com.example.finance.dto.response;

import java.math.BigDecimal;

public record MonthlyFlowItem(
        int month,
        String monthName,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netFlow
) {
}
