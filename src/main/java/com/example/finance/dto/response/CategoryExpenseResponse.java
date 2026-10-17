package com.example.finance.dto.response;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        String category,
        BigDecimal totalAmount,
        Double percentage
) {
    public CategoryExpenseResponse(String category, BigDecimal totalAmount) {
        this(category, totalAmount, 0.0);
    }
}