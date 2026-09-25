package com.example.finance.controller;

import com.example.finance.dto.request.AccountRequest;
import com.example.finance.dto.request.AccountUpdateRequest;
import com.example.finance.dto.response.AccountResponse;
import com.example.finance.service.AccountService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public AccountResponse createAccount(
            @Valid @RequestBody AccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping
    public List<AccountResponse> getAccounts() {
        return accountService.getAccounts();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccountById(
            @PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/user/{userId}")
    public List<AccountResponse> getAccountsByUserId(
            @PathVariable Long userId) {
        return accountService.getAccountsByUserId(userId);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id) {
        accountService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }
}