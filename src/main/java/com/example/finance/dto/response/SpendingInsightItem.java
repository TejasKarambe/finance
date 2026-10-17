package com.example.finance.dto.response;

import java.math.BigDecimal;

public record SpendingInsightItem(
        String id,
        String type, // "SAVINGS_OPPORTUNITY", "ANOMALY_ALERT", "SUBSCRIPTION_LOAD", "POSITIVE_TREND"
        String title,
        String description,
        BigDecimal potentialMonthlySavings,
        String impactLevel, // "HIGH", "MEDIUM", "LOW"
        String recommendedAction
) {
}
