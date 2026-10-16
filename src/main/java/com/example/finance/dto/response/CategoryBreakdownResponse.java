package com.example.finance.dto.response;

import java.math.BigDecimal;

public record CategoryBreakdownResponse(
        String category,
        BigDecimal totalAmount,
        Double percentage
) {
}
