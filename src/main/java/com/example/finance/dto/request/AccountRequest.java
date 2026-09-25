package com.example.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(

        @NotBlank(message = "Account name is required")
        String name,

        @NotBlank(message = "Account type is required")
        String type,

        @NotNull(message = "Balance is required")
        @DecimalMin(value = "0.0", message = "Balance cannot be negative")
        BigDecimal balance,

        @NotNull(message = "User ID is required")
        Long userId

) {
}