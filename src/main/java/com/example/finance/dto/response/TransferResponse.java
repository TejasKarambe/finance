package com.example.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        String message,
        Long sourceAccountId,
        String sourceAccountName,
        BigDecimal sourceNewBalance,
        Long destinationAccountId,
        String destinationAccountName,
        BigDecimal destinationNewBalance,
        BigDecimal amount,
        LocalDateTime timestamp
) {
}
