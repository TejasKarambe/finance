package com.example.finance.dto.response;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        String category,
        BigDecimal totalAmount
) {
}