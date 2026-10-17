package com.example.finance.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        String title,
        String message,
        String type, // "WARNING", "ALERT", "INFO", "SUCCESS"
        String category, // "BUDGET", "RECURRING", "ACCOUNT", "GOAL", "SYSTEM"
        LocalDateTime timestamp,
        String actionUrl,
        boolean read
) {
}
