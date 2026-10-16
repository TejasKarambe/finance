package com.example.finance.service;

import com.example.finance.dto.request.TransactionFilterRequest;
import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.request.TransactionUpdateRequest;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.AccountNotFoundException;
import com.example.finance.exception.InsufficientBalanceException;
import com.example.finance.exception.TransactionNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.specification.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        Account account;
        if (userId != null) {
            account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                    .orElseThrow(() -> new AccountNotFoundException("Account Not Found or access denied"));
        } else {
            account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new AccountNotFoundException("Account Not Found"));
        }

        if (request.type() == TransactionType.EXPENSE
                && account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient account balance");
        }

        if (request.type() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(request.amount()));
        } else if (request.type() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(request.amount()));
        }

        String paymentMethod = request.paymentMethod() != null && !request.paymentMethod().isBlank()
                ? request.paymentMethod()
                : "OTHER";

        Transaction transaction = new Transaction(
                request.amount(),
                request.type(),
                request.category(),
                request.description(),
                paymentMethod,
                request.transactionDate(),
                LocalDateTime.now(),
                account,
                account.getUser()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);

        return mapToResponse(savedTransaction);
    }

    public TransactionResponse createTransaction(TransactionRequest request) {
        return createTransaction(request, null);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter, Pageable pageable, Long userId) {
        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();

        if (userId != null) {
            spec = spec.and(TransactionSpecification.hasUserId(userId));
        }

        if (filter != null) {
            if (filter.search() != null && !filter.search().isBlank()) {
                spec = spec.and(TransactionSpecification.descriptionContains(filter.search()));
            }
            if (filter.type() != null) {
                spec = spec.and(TransactionSpecification.hasType(filter.type()));
            }
            if (filter.category() != null && !filter.category().isBlank()) {
                spec = spec.and(TransactionSpecification.hasCategory(filter.category()));
            }
            if (filter.accountId() != null) {
                spec = spec.and(TransactionSpecification.hasAccountId(filter.accountId()));
            }
            if (filter.minAmount() != null) {
                spec = spec.and(TransactionSpecification.amountGreaterThanOrEqualTo(filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                spec = spec.and(TransactionSpecification.amountLessThanOrEqualTo(filter.maxAmount()));
            }
            if (filter.startDate() != null) {
                spec = spec.and(TransactionSpecification.dateGreaterThanOrEqualTo(filter.startDate()));
            }
            if (filter.endDate() != null) {
                spec = spec.and(TransactionSpecification.dateLessThanOrEqualTo(filter.endDate()));
            }
        }

        return transactionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public Page<TransactionResponse> getTransactions(Pageable pageable) {
        return getTransactions(null, pageable, null);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id, Long userId) {
        Transaction transaction;
        if (userId != null) {
            transaction = transactionRepository.findByIdAndAccountUserId(id, userId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        } else {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        }
        return mapToResponse(transaction);
    }

    public TransactionResponse getTransactionById(Long id) {
        return getTransactionById(id, null);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request, Long userId) {
        Transaction transaction;
        if (userId != null) {
            transaction = transactionRepository.findByIdAndAccountUserId(id, userId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        } else {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        }

        Account account = transaction.getAccount();

        // 1. Reverse the OLD transaction effect
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else if (transaction.getType() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }

        // 2. Validate the NEW transaction
        if (request.type() == TransactionType.EXPENSE && account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient account balance");
        }

        // 3. Apply the NEW transaction effect
        if (request.type() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(request.amount()));
        } else if (request.type() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(request.amount()));
        }

        // 4. Update fields
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        if (request.paymentMethod() != null) {
            transaction.setPaymentMethod(request.paymentMethod());
        }
        transaction.setTransactionDate(request.transactionDate());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);

        return mapToResponse(updatedTransaction);
    }

    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        return updateTransaction(id, request, null);
    }

    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        Transaction transaction;
        if (userId != null) {
            transaction = transactionRepository.findByIdAndAccountUserId(id, userId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        } else {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        }

        Account account = transaction.getAccount();

        // Reverse balance impact
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else if (transaction.getType() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }

        transactionRepository.delete(transaction);
        accountRepository.save(account);
    }

    public void deleteTransaction(Long id) {
        deleteTransaction(id, null);
    }

    public List<TransactionResponse> getTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByCategory(String category) {
        return transactionRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByAccountAndType(Long accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<TransactionResponse> searchTransactions(TransactionFilterRequest filter, Pageable pageable) {
        return getTransactions(filter, pageable, null);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        String accountName = transaction.getAccount() != null ? transaction.getAccount().getName() : "";
        Long userId = transaction.getUser() != null
                ? transaction.getUser().getId()
                : (transaction.getAccount() != null && transaction.getAccount().getUser() != null
                        ? transaction.getAccount().getUser().getId()
                        : null);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getPaymentMethod(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getAccount() != null ? transaction.getAccount().getId() : null,
                accountName,
                userId
        );
    }
}