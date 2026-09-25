package com.example.finance.service;

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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public TransactionResponse createTransaction(
            TransactionRequest request) {

        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found"));

        Transaction transaction = new Transaction(
                request.amount(),
                request.type(),
                request.category(),
                request.description(),
                request.transactionDate(),
                LocalDateTime.now(),
                account);

        // this is used to update the balance of the account based on the transaction
        // type
        if (request.type() == TransactionType.EXPENSE
                && account.getBalance().compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException(
                    "Insufficient account balance");
        }

        if (request.type() == TransactionType.INCOME) {

            account.setBalance(
                    account.getBalance().add(request.amount()));

        } else {

            account.setBalance(
                    account.getBalance().subtract(request.amount()));
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        accountRepository.save(account);

        return mapToResponse(savedTransaction);
    }

    public List<TransactionResponse> getTransactions() {

        return transactionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TransactionResponse getTransactionById(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction Not Found"));

        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(
            Long id,
            TransactionUpdateRequest request) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction Not Found"));

        Account account = transaction.getAccount();

        /*
         * Step 1:
         * Reverse the OLD transaction effect.
         */
        if (transaction.getType() == TransactionType.INCOME) {

            account.setBalance(
                    account.getBalance()
                            .subtract(transaction.getAmount()));

        } else {

            account.setBalance(
                    account.getBalance()
                            .add(transaction.getAmount()));
        }

        /*
         * Step 2:
         * Validate the NEW transaction.
         */
        if (request.type() == TransactionType.EXPENSE
                && account.getBalance()
                        .compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException(
                    "Insufficient account balance");
        }

        /*
         * Step 3:
         * Apply the NEW transaction effect.
         */
        if (request.type() == TransactionType.INCOME) {

            account.setBalance(
                    account.getBalance()
                            .add(request.amount()));

        } else {

            account.setBalance(
                    account.getBalance()
                            .subtract(request.amount()));
        }

        /*
         * Step 4:
         * Update transaction fields.
         */
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());

        /*
         * Step 5:
         * Persist both changes.
         */
        Transaction updatedTransaction = transactionRepository.save(transaction);

        accountRepository.save(account);

        return mapToResponse(updatedTransaction);
    }

    private TransactionResponse mapToResponse(
            Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getAccount().getId());
    }

    @Transactional
    public void deleteTransaction(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction Not Found"));

        Account account = transaction.getAccount();

        // Reverse the transaction's effect on account balance
        if (transaction.getType() == TransactionType.INCOME) {

            account.setBalance(
                    account.getBalance()
                            .subtract(transaction.getAmount()));

        } else {

            account.setBalance(
                    account.getBalance()
                            .add(transaction.getAmount()));
        }

        transactionRepository.delete(transaction);

        accountRepository.save(account);
    }

    public List<TransactionResponse> getTransactionsByType(
            TransactionType type) {
        return transactionRepository.findByType(type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByCategory(
            String category) {
        return transactionRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByAccountId(
            Long accountId) {
        return transactionRepository.findByAccountId(accountId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByAccountAndType(
            Long accountId,
            TransactionType type) {
        return transactionRepository
                .findByAccountIdAndType(accountId, type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<TransactionResponse> getTransactions(
            Pageable pageable) {

        return transactionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

}