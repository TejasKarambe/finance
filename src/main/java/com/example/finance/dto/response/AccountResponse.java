package com.example.finance.dto.response;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String name,
        String type,
        BigDecimal balance,
        BigDecimal openingBalance,
        String currency,
        Long userId
) {
    public AccountResponse(Long id, String name, String type, BigDecimal balance, Long userId) {
        this(id, name, type, balance, balance, "USD", userId);
    }
}