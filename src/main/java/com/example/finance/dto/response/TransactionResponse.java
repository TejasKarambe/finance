package com.example.finance.dto.response;

import com.example.finance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        String category,
        String description,
        String paymentMethod,
        LocalDate transactionDate,
        LocalDateTime createdAt,
        Long accountId,
        String accountName,
        Long userId
) {
    public TransactionResponse(
            Long id,
            BigDecimal amount,
            TransactionType type,
            String category,
            String description,
            LocalDate transactionDate,
            LocalDateTime createdAt,
            Long accountId) {
        this(id, amount, type, category, description, "OTHER", transactionDate, createdAt, accountId, "", null);
    }
}