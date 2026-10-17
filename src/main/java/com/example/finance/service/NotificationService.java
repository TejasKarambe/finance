package com.example.finance.service;

import com.example.finance.dto.response.BudgetStatusResponse;
import com.example.finance.dto.response.NotificationResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.RecurringTransaction;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.RecurringTransactionRepository;
import com.example.finance.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final BudgetService budgetService;
    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final SavingsGoalRepository goalRepository;

    public NotificationService(
            BudgetService budgetService,
            RecurringTransactionRepository recurringRepository,
            AccountRepository accountRepository,
            SavingsGoalRepository goalRepository) {
        this.budgetService = budgetService;
        this.recurringRepository = recurringRepository;
        this.accountRepository = accountRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        List<NotificationResponse> notifications = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Budget Alerts
        try {
            List<BudgetStatusResponse> budgetStatuses = budgetService.getBudgetStatuses(userId, today.getMonthValue(), today.getYear());
            for (BudgetStatusResponse b : budgetStatuses) {
                if ("EXCEEDED".equalsIgnoreCase(b.status())) {
                    notifications.add(new NotificationResponse(
                            "budget-exceeded-" + b.category(),
                            "Budget Exceeded: " + b.category(),
                            String.format("You have spent $%.2f out of your $%.2f limit (%.1f%%).",
                                    b.spentAmount(), b.limitAmount(), b.percentageSpent()),
                            "ALERT",
                            "BUDGET",
                            LocalDateTime.now(),
                            "/budgets",
                            false
                    ));
                } else if ("WARNING".equalsIgnoreCase(b.status())) {
                    notifications.add(new NotificationResponse(
                            "budget-warning-" + b.category(),
                            "Budget Warning: " + b.category(),
                            String.format("You are at %.1f%% of your $%.2f monthly budget ($%.2f spent).",
                                    b.percentageSpent(), b.limitAmount(), b.spentAmount()),
                            "WARNING",
                            "BUDGET",
                            LocalDateTime.now(),
                            "/budgets",
                            false
                    ));
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Upcoming Recurring Transactions (Due in <= 7 days)
        try {
            LocalDate sevenDaysLater = today.plusDays(7);
            List<RecurringTransaction> recurringList = recurringRepository.findByUserIdAndActiveTrueOrderByNextDueDateAsc(userId);
            for (RecurringTransaction r : recurringList) {
                if (r.getNextDueDate() != null && !r.getNextDueDate().isAfter(sevenDaysLater)) {
                    long days = ChronoUnit.DAYS.between(today, r.getNextDueDate());
                    String dueText = days <= 0 ? "is due today!" : "is due in " + days + " day" + (days > 1 ? "s" : "") + ".";
                    String type = days <= 2 ? "ALERT" : "INFO";

                    notifications.add(new NotificationResponse(
                            "recurring-due-" + r.getId(),
                            "Upcoming Payment: " + r.getTitle(),
                            String.format("%s of $%.2f %s", r.getTitle(), r.getAmount(), dueText),
                            type,
                            "RECURRING",
                            LocalDateTime.now(),
                            "/recurring",
                            false
                    ));
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Low Account Balance Alerts (< $100)
        try {
            List<Account> accounts = accountRepository.findByUserId(userId);
            for (Account acc : accounts) {
                if (acc.getBalance().compareTo(BigDecimal.valueOf(100)) < 0) {
                    notifications.add(new NotificationResponse(
                            "account-low-" + acc.getId(),
                            "Low Account Balance: " + acc.getName(),
                            String.format("Account %s balance is currently $%.2f.", acc.getName(), acc.getBalance()),
                            "WARNING",
                            "ACCOUNT",
                            LocalDateTime.now(),
                            "/accounts",
                            false
                    ));
                }
            }
        } catch (Exception ignored) {
        }

        // 4. Savings Goal Milestones
        try {
            List<SavingsGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
            for (SavingsGoal g : goals) {
                if (g.isCompleted()) {
                    notifications.add(new NotificationResponse(
                            "goal-completed-" + g.getId(),
                            "Goal Achieved: " + g.getName(),
                            String.format("Congratulations! You've reached your $%.2f target for %s!", g.getTargetAmount(), g.getName()),
                            "SUCCESS",
                            "GOAL",
                            LocalDateTime.now(),
                            "/goals",
                            false
                    ));
                }
            }
        } catch (Exception ignored) {
        }

        return notifications;
    }
}
