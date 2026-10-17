package com.example.finance.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FullDataBackupResponse(
        LocalDateTime exportedAt,
        Long userId,
        List<AccountResponse> accounts,
        List<TransactionResponse> transactions,
        List<BudgetResponse> budgets,
        List<RecurringTransactionResponse> recurringTransactions,
        List<SavingsGoalResponse> savingsGoals
) {
}
