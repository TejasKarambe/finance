package com.example.finance.dto.request;

import com.example.finance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionFilterRequest(
        String search,
        TransactionType type,
        String category,
        Long accountId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDate startDate,
        LocalDate endDate
) {
    public TransactionFilterRequest(
            TransactionType type,
            String category,
            Long accountId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate) {
        this(null, type, category, accountId, minAmount, maxAmount, startDate, endDate);
    }
}