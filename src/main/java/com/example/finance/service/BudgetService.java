package com.example.finance.service;

import com.example.finance.dto.request.BudgetRequest;
import com.example.finance.dto.response.BudgetResponse;
import com.example.finance.dto.response.BudgetStatusResponse;
import com.example.finance.entity.Budget;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public BudgetResponse createOrUpdateBudget(BudgetRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Budget> existingOpt = budgetRepository.findByUserIdAndCategoryAndMonthAndYear(
                userId, request.category(), request.month(), request.year());

        Budget budget;
        if (existingOpt.isPresent()) {
            budget = existingOpt.get();
            budget.setLimitAmount(request.limitAmount());
        } else {
            budget = new Budget(user, request.category(), request.limitAmount(), request.month(), request.year());
        }

        Budget saved = budgetRepository.save(budget);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long userId, Integer month, Integer year) {
        List<Budget> budgets;
        if (month != null && year != null) {
            budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        } else {
            budgets = budgetRepository.findByUserId(userId);
        }
        return budgets.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        return mapToResponse(budget);
    }

    public void deleteBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> getBudgetStatuses(Long userId, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        List<BudgetStatusResponse> statusList = new ArrayList<>();

        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository.getTotalByTypeAndCategoryAndDateRangeAndUserId(
                    userId, TransactionType.EXPENSE, budget.getCategory(), start, end);
            if (spent == null) {
                spent = BigDecimal.ZERO;
            }

            BigDecimal limit = budget.getLimitAmount();
            BigDecimal remaining = limit.subtract(spent);

            double percentage = 0.0;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                percentage = spent.multiply(BigDecimal.valueOf(100))
                        .divide(limit, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            String status;
            String message;

            if (percentage >= 100.0) {
                status = "EXCEEDED";
                BigDecimal overAmount = spent.subtract(limit);
                message = "Budget exceeded by $" + overAmount.setScale(2, RoundingMode.HALF_UP);
            } else if (percentage >= 80.0) {
                status = "WARNING";
                message = "Warning: " + String.format("%.1f", percentage) + "% of budget reached ($" +
                        remaining.setScale(2, RoundingMode.HALF_UP) + " remaining)";
            } else {
                status = "NORMAL";
                message = "Within budget ($" + remaining.setScale(2, RoundingMode.HALF_UP) + " remaining)";
            }

            statusList.add(new BudgetStatusResponse(
                    budget.getId(),
                    budget.getCategory(),
                    limit,
                    spent,
                    remaining,
                    percentage,
                    status,
                    message
            ));
        }

        statusList.sort((a, b) -> {
            int scoreA = getStatusSeverity(a.status());
            int scoreB = getStatusSeverity(b.status());
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA);
            }
            return Double.compare(b.percentageSpent(), a.percentageSpent());
        });

        return statusList;
    }

    private int getStatusSeverity(String status) {
        return switch (status) {
            case "EXCEEDED" -> 3;
            case "WARNING" -> 2;
            default -> 1;
        };
    }

    private BudgetResponse mapToResponse(Budget b) {
        return new BudgetResponse(
                b.getId(),
                b.getCategory(),
                b.getLimitAmount(),
                b.getMonth(),
                b.getYear(),
                b.getUser().getId(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
