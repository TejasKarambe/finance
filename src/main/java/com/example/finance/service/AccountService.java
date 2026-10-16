package com.example.finance.service;

import com.example.finance.dto.request.AccountRequest;
import com.example.finance.dto.request.AccountUpdateRequest;
import com.example.finance.dto.request.TransferRequest;
import com.example.finance.dto.response.AccountResponse;
import com.example.finance.dto.response.TransferResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.AccountNotFoundException;
import com.example.finance.exception.InsufficientBalanceException;
import com.example.finance.exception.UserNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request, Long authenticatedUserId) {
        Long targetUserId = authenticatedUserId != null ? authenticatedUserId : request.userId();
        if (targetUserId == null) {
            throw new IllegalArgumentException("User ID is required to create an account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));

        BigDecimal balance = request.balance() != null ? request.balance() : BigDecimal.ZERO;
        BigDecimal openingBalance = request.openingBalance() != null ? request.openingBalance() : balance;
        String currency = request.currency() != null && !request.currency().isBlank() ? request.currency() : "USD";

        Account account = new Account(
                request.name(),
                request.type(),
                balance,
                openingBalance,
                currency,
                user
        );

        Account savedAccount = accountRepository.save(account);
        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found with id: " + id));

        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountUpdateRequest request, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found with id: " + id));

        account.setName(request.name());
        account.setType(request.type());
        if (request.balance() != null) {
            account.setBalance(request.balance());
        }

        Account updatedAccount = accountRepository.save(account);
        return mapToResponse(updatedAccount);
    }

    @Transactional
    public void deleteAccount(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found with id: " + id));

        accountRepository.delete(account);
    }

    @Transactional
    public TransferResponse transferFunds(TransferRequest request, Long userId) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        Account sourceAccount = accountRepository.findByIdAndUserId(request.sourceAccountId(), userId)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found or access denied"));

        Account destinationAccount = accountRepository.findByIdAndUserId(request.destinationAccountId(), userId)
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found or access denied"));

        if (sourceAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account: " + sourceAccount.getName());
        }

        // Deduct from source account
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));

        // Add to destination account
        destinationAccount.setBalance(destinationAccount.getBalance().add(request.amount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        LocalDate txDate = request.transferDate() != null ? request.transferDate() : LocalDate.now();
        String desc = request.description() != null && !request.description().isBlank()
                ? request.description()
                : "Transfer from " + sourceAccount.getName() + " to " + destinationAccount.getName();

        // Record transfer transaction on source account
        Transaction transferTx = new Transaction(
                request.amount(),
                TransactionType.TRANSFER,
                "Transfer",
                desc,
                txDate,
                LocalDateTime.now(),
                sourceAccount
        );
        transactionRepository.save(transferTx);

        return new TransferResponse(
                "Transfer of $" + request.amount() + " completed successfully",
                sourceAccount.getId(),
                sourceAccount.getName(),
                sourceAccount.getBalance(),
                destinationAccount.getId(),
                destinationAccount.getName(),
                destinationAccount.getBalance(),
                request.amount(),
                LocalDateTime.now()
        );
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getBalance(),
                account.getOpeningBalance(),
                account.getCurrency(),
                account.getUser().getId()
        );
    }
}