package com.example.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(
        @NotNull(message = "Source account ID is required")
        Long sourceAccountId,

        @NotNull(message = "Destination account ID is required")
        Long destinationAccountId,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
        BigDecimal amount,

        String description,

        LocalDate transferDate
) {
}
