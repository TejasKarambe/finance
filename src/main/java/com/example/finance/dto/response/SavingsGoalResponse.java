package com.example.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavingsGoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        Double progressPercentage,
        LocalDate targetDate,
        Long daysRemaining,
        String category,
        String color,
        String icon,
        boolean completed,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
