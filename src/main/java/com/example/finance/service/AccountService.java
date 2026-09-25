package com.example.finance.service;

import com.example.finance.dto.request.AccountRequest;
import com.example.finance.dto.request.AccountUpdateRequest;
import com.example.finance.dto.response.AccountResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.User;
import com.example.finance.exception.AccountNotFoundException;
import com.example.finance.exception.UserNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public AccountResponse createAccount(AccountRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));

        Account account = new Account(
                request.name(),
                request.type(),
                request.balance(),
                user);

        Account savedAccount = accountRepository.save(account);

        return mapToResponse(savedAccount);
    }

    public List<AccountResponse> getAccounts() {

        return accountRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AccountResponse mapToResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getBalance(),
                account.getUser().getId());
    }

    public AccountResponse getAccountById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found"));

        return mapToResponse(account);
    }

    public List<AccountResponse> getAccountsByUserId(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User Not Found");
        }

        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse updateAccount(
            Long id,
            AccountUpdateRequest request) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found"));

        account.setName(request.name());
        account.setType(request.type());
        account.setBalance(request.balance());

        Account updatedAccount = accountRepository.save(account);

        return mapToResponse(updatedAccount);
    }

    public void deleteAccount(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account Not Found"));

        accountRepository.delete(account);
    }
}