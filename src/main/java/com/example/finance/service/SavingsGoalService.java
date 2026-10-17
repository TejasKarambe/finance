package com.example.finance.service;

import com.example.finance.dto.request.GoalContributionRequest;
import com.example.finance.dto.request.SavingsGoalRequest;
import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.response.SavingsGoalResponse;
import com.example.finance.entity.*;
import com.example.finance.exception.AccountNotFoundException;
import com.example.finance.exception.InsufficientBalanceException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.SavingsGoalRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public SavingsGoalService(
            SavingsGoalRepository goalRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionService transactionService) {
        this.goalRepository = goalRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getGoals(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getGoal(Long userId, Long id) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));
        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse createGoal(Long userId, SavingsGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount() != null ? request.currentAmount() : BigDecimal.ZERO);
        goal.setTargetDate(request.targetDate());
        goal.setCategory(request.category() != null ? request.category() : "General");
        goal.setColor(request.color() != null ? request.color() : "#4fb8b2");
        goal.setIcon(request.icon() != null ? request.icon() : "target");
        goal.setNotes(request.notes());

        SavingsGoal saved = goalRepository.save(goal);
        return mapToResponse(saved);
    }

    @Transactional
    public SavingsGoalResponse updateGoal(Long userId, Long id, SavingsGoalRequest request) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        if (request.currentAmount() != null) {
            goal.setCurrentAmount(request.currentAmount());
        }
        goal.setTargetDate(request.targetDate());
        if (request.category() != null) {
            goal.setCategory(request.category());
        }
        if (request.color() != null) {
            goal.setColor(request.color());
        }
        if (request.icon() != null) {
            goal.setIcon(request.icon());
        }
        goal.setNotes(request.notes());

        SavingsGoal updated = goalRepository.save(goal);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteGoal(Long userId, Long id) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));
        goalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoalResponse contribute(Long userId, Long id, GoalContributionRequest request) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found or access denied"));

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient funds in account to contribute to goal");
        }

        // Post transaction record
        String note = request.notes() != null && !request.notes().isBlank()
                ? " - " + request.notes()
                : "";
        TransactionRequest txRequest = new TransactionRequest(
                request.amount(),
                TransactionType.EXPENSE,
                "Savings",
                "Deposit to Savings Goal: " + goal.getName() + note,
                "TRANSFER",
                LocalDate.now(),
                account.getId()
        );

        transactionService.createTransaction(txRequest, userId);

        // Update goal balance
        goal.setCurrentAmount(goal.getCurrentAmount().add(request.amount()));
        goal.checkCompletion();

        SavingsGoal saved = goalRepository.save(goal);
        return mapToResponse(saved);
    }

    private SavingsGoalResponse mapToResponse(SavingsGoal goal) {
        BigDecimal target = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = target.subtract(current);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        double percentage = 0.0;
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            percentage = current.multiply(BigDecimal.valueOf(100))
                    .divide(target, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Long daysRemaining = null;
        if (goal.getTargetDate() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        }

        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                target,
                current,
                remaining,
                percentage,
                goal.getTargetDate(),
                daysRemaining,
                goal.getCategory(),
                goal.getColor(),
                goal.getIcon(),
                goal.isCompleted(),
                goal.getNotes(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
