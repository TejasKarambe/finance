package com.example.finance.dto.request;

import com.example.finance.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotBlank(message = "Category is required")
        String category,

        String description,

        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate
) {
}