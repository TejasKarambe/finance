package com.example.finance.dto.response;

import com.example.finance.entity.RecurrenceFrequency;
import com.example.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecurringTransactionResponse(
        Long id,
        Long accountId,
        String accountName,
        String title,
        BigDecimal amount,
        TransactionType type,
        String category,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate nextDueDate,
        LocalDate endDate,
        String paymentMethod,
        String description,
        boolean active,
        boolean autoProcess,
        LocalDate lastProcessedDate,
        long daysUntilDue,
        boolean isDue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
