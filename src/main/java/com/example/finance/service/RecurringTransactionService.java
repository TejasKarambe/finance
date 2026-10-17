package com.example.finance.service;

import com.example.finance.dto.request.RecurringTransactionRequest;
import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.response.RecurringTransactionResponse;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.entity.*;
import com.example.finance.exception.AccountNotFoundException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.RecurringTransactionRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionService transactionService) {
        this.recurringRepository = recurringRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getRecurringTransactions(Long userId) {
        return recurringRepository.findByUserIdOrderByNextDueDateAsc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransaction(Long userId, Long id) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));
        return mapToResponse(entity);
    }

    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(Long userId, RecurringTransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found or access denied"));

        RecurringTransaction entity = new RecurringTransaction();
        entity.setUser(user);
        entity.setAccount(account);
        entity.setTitle(request.title());
        entity.setAmount(request.amount());
        entity.setType(request.type());
        entity.setCategory(request.category());
        entity.setFrequency(request.frequency());
        entity.setStartDate(request.startDate());
        entity.setNextDueDate(request.nextDueDate() != null ? request.nextDueDate() : request.startDate());
        entity.setEndDate(request.endDate());
        entity.setPaymentMethod(request.paymentMethod() != null ? request.paymentMethod() : "OTHER");
        entity.setDescription(request.description());
        entity.setActive(request.active() != null ? request.active() : true);
        entity.setAutoProcess(request.autoProcess() != null ? request.autoProcess() : false);

        RecurringTransaction saved = recurringRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(Long userId, Long id, RecurringTransactionRequest request) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!entity.getAccount().getId().equals(request.accountId())) {
            Account newAccount = accountRepository.findByIdAndUserId(request.accountId(), userId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found or access denied"));
            entity.setAccount(newAccount);
        }

        entity.setTitle(request.title());
        entity.setAmount(request.amount());
        entity.setType(request.type());
        entity.setCategory(request.category());
        entity.setFrequency(request.frequency());
        entity.setStartDate(request.startDate());
        if (request.nextDueDate() != null) {
            entity.setNextDueDate(request.nextDueDate());
        }
        entity.setEndDate(request.endDate());
        if (request.paymentMethod() != null) {
            entity.setPaymentMethod(request.paymentMethod());
        }
        entity.setDescription(request.description());
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        if (request.autoProcess() != null) {
            entity.setAutoProcess(request.autoProcess());
        }

        RecurringTransaction updated = recurringRepository.save(entity);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteRecurringTransaction(Long userId, Long id) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));
        recurringRepository.delete(entity);
    }

    @Transactional
    public TransactionResponse processRecurringTransaction(Long userId, Long id) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        LocalDate txDate = entity.getNextDueDate() != null ? entity.getNextDueDate() : LocalDate.now();

        TransactionRequest txRequest = new TransactionRequest(
                entity.getAmount(),
                entity.getType(),
                entity.getCategory(),
                entity.getTitle() + (entity.getDescription() != null && !entity.getDescription().isBlank() ? " - " + entity.getDescription() : ""),
                entity.getPaymentMethod(),
                txDate,
                entity.getAccount().getId()
        );

        TransactionResponse createdTx = transactionService.createTransaction(txRequest, userId);

        entity.setLastProcessedDate(txDate);
        LocalDate nextDate = advanceDueDate(entity.getNextDueDate(), entity.getFrequency());
        entity.setNextDueDate(nextDate);

        if (entity.getEndDate() != null && nextDate.isAfter(entity.getEndDate())) {
            entity.setActive(false);
        }

        recurringRepository.save(entity);
        return createdTx;
    }

    @Transactional
    public List<TransactionResponse> processAllDue(Long userId) {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueItems = recurringRepository.findDueForUser(userId, today);
        List<TransactionResponse> processed = new ArrayList<>();

        for (RecurringTransaction item : dueItems) {
            TransactionResponse tx = processRecurringTransaction(userId, item.getId());
            processed.add(tx);
        }

        return processed;
    }

    private LocalDate advanceDueDate(LocalDate current, RecurrenceFrequency frequency) {
        if (current == null) current = LocalDate.now();
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
        };
    }

    private RecurringTransactionResponse mapToResponse(RecurringTransaction entity) {
        LocalDate today = LocalDate.now();
        long daysUntilDue = 0;
        boolean isDue = false;

        if (entity.getNextDueDate() != null) {
            daysUntilDue = ChronoUnit.DAYS.between(today, entity.getNextDueDate());
            isDue = !entity.getNextDueDate().isAfter(today);
        }

        return new RecurringTransactionResponse(
                entity.getId(),
                entity.getAccount() != null ? entity.getAccount().getId() : null,
                entity.getAccount() != null ? entity.getAccount().getName() : "N/A",
                entity.getTitle(),
                entity.getAmount(),
                entity.getType(),
                entity.getCategory(),
                entity.getFrequency(),
                entity.getStartDate(),
                entity.getNextDueDate(),
                entity.getEndDate(),
                entity.getPaymentMethod(),
                entity.getDescription(),
                entity.isActive(),
                entity.isAutoProcess(),
                entity.getLastProcessedDate(),
                daysUntilDue,
                isDue,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
