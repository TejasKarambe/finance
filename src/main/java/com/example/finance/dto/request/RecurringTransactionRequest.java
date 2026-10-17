package com.example.finance.dto.request;

import com.example.finance.entity.RecurrenceFrequency;
import com.example.finance.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(
        @NotNull(message = "Account ID is required")
        Long accountId,

        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotBlank(message = "Category is required")
        String category,

        @NotNull(message = "Frequency is required")
        RecurrenceFrequency frequency,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate nextDueDate,

        LocalDate endDate,

        String paymentMethod,

        String description,

        Boolean active,

        Boolean autoProcess
) {
}
