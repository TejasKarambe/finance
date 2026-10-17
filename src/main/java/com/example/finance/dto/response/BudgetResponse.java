package com.example.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetResponse(
        Long id,
        String category,
        BigDecimal limitAmount,
        int month,
        int year,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
