package com.example.finance.dto.response;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String name,
        String type,
        BigDecimal balance,
        Long userId
) {
}