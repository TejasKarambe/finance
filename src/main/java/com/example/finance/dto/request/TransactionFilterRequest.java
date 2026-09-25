package com.example.finance.dto.request;

import com.example.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionFilterRequest(
        TransactionType type,
        String category,
        Long accountId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDate startDate,
        LocalDate endDate
) {
}